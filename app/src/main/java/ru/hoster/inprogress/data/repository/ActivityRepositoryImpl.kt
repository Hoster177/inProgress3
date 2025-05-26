package ru.hoster.inprogress.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.hoster.inprogress.data.ActivityItem
import ru.hoster.inprogress.domain.model.ActivityRepository
import ru.hoster.inprogress.data.local.ActivityDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import ru.hoster.inprogress.domain.model.AuthService
import java.util.Calendar // Для фильтрации по дате

// Убедись, что импортировано
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach // Для логирования эмиссий
import java.util.Date // Для форматирования в логах
import java.text.SimpleDateFormat // Для форматирования в логах
import java.util.Locale // Для форматирования в логах

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val activityDao: ActivityDao,
    private val authService: AuthService,
    private val firestore: FirebaseFirestore // Убедись, что FirebaseFirestore предоставляется через Hilt (например, в AppModule)
) : ActivityRepository {

    override suspend fun addActivity(activity: ActivityItem) {
        try {
            // 1. Сохраняем в локальную базу данных Room
            // createdAt уже установлен в ActivityItem при создании
            val localId = activityDao.insertActivity(activity)
            val activityWithLocalId = activity.copy(id = localId) // Обновляем объект с локальным ID

            Log.d("ActivityRepo", "Activity inserted locally with ID: $localId")

            // 2. Сохраняем в Firestore (если userId есть)
            if (activityWithLocalId.userId.isNotBlank()) {
                // Создаем копию для Firestore, чтобы избежать сохранения локального id в Firestore, если не нужно
                // firebaseId будет сгенерирован Firestore автоматически
                val firestoreActivityMap = activityWithLocalId.toFirestoreMap()

                val documentReference = firestore.collection("users")
                    .document(activityWithLocalId.userId)
                    .collection("activities")
                    .add(firestoreActivityMap)
                    .await()

                val firebaseId = documentReference.id
                Log.d("ActivityRepo", "Activity added to Firestore with ID: $firebaseId")

                // 3. Обновляем локальную запись с firebaseId, полученным от Firestore
                activityDao.updateActivity(activityWithLocalId.copy(firebaseId = firebaseId))
                Log.d("ActivityRepo", "Local activity updated with firebaseId: $firebaseId")
            } else {
                Log.w("ActivityRepo", "UserID is blank, activity only saved locally.")
            }
        } catch (e: Exception) {
            Log.e("ActivityRepo", "Error adding activity", e)
            throw e // Пробрасываем, чтобы ViewModel обработал
        }
    }

    override suspend fun updateActivity(activity: ActivityItem) {
        try {
            // Обновляем в Room
            activityDao.updateActivity(activity)
            Log.d("ActivityRepo", "Activity updated locally with ID: ${activity.id}, FirebaseID: ${activity.firebaseId}")

            // Обновляем в Firestore, если есть firebaseId и userId
            if (activity.userId.isNotBlank() && activity.firebaseId != null && activity.firebaseId.isNotBlank()) {
                firestore.collection("users")
                    .document(activity.userId)
                    .collection("activities")
                    .document(activity.firebaseId)
                    .set(activity.toFirestoreMap(), SetOptions.merge()) // SetOptions.merge() для неполного обновления
                    .await()
                Log.d("ActivityRepo", "Activity updated in Firestore with ID: ${activity.firebaseId}")
            } else {
                Log.w("ActivityRepo", "UserID or FirebaseID is blank, activity only updated locally or not synced.")
            }
        } catch (e: Exception) {
            Log.e("ActivityRepo", "Error updating activity", e)
            throw e
        }
    }

    // Пример маппера в Map для Firestore. Можно вынести в отдельный файл или companion object.
    private fun ActivityItem.toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            // "id" - локальный ID, обычно не храним в Firestore, если firebaseId основной
            "firebaseId" to firebaseId, // Может быть null при первом добавлении, Firestore сгенерирует свой
            "userId" to userId,
            "name" to name,
            "totalDurationMillisToday" to totalDurationMillisToday,
            "isActive" to isActive,
            "colorHex" to colorHex,
            "createdAt" to createdAt // Firestore @ServerTimestamp сработает, если это FieldValue.serverTimestamp() или Date
            // "lastStartTime" - если есть такое поле, тоже добавь
        )
    }


    override fun getActivitiesForTodayFlow(): Flow<List<ActivityItem>> {
        val currentUserId = authService.getCurrentUserId()

        if (currentUserId == null) {
            Log.e("ActivityRepo", "getActivitiesForTodayFlow: No current user ID. Returning empty list.")
            return flowOf(emptyList())
        }

        Log.d("ActivityRepo", "getActivitiesForTodayFlow: Called for user ID: $currentUserId")

        return activityDao.getActivitiesForUserFlow(currentUserId)
            .onEach { activitiesFromDao ->
                Log.d("ActivityRepo", "getActivitiesForTodayFlow: DAO emitted ${activitiesFromDao.size} activities for user $currentUserId.")
                // Логируем первые несколько активностей из DAO для проверки
                activitiesFromDao.take(3).forEach { act ->
                    Log.d("ActivityRepo", "DAO item: Name='${act.name}', CreatedAt=${formatLogDate(act.createdAt)}, IsActive=${act.isActive}, Duration=${act.totalDurationMillisToday}")
                }
            }
            .map { activities ->
                val calendar = Calendar.getInstance()
                val sdfLog = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

                val todayStartMillis = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val todayEndMillis = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                Log.d("ActivityRepo", "Filtering for today: Start=${formatLogDate(Date(todayStartMillis))} (${todayStartMillis}), End=${formatLogDate(Date(todayEndMillis))} (${todayEndMillis})")

                val filteredActivities = activities.filter { activity ->
                    val activityCreatedAt = activity.createdAt.time
                    val isInRange = activityCreatedAt in todayStartMillis..todayEndMillis
                    Log.v("ActivityRepo", "Filtering item: Name='${activity.name}', CreatedAt=${formatLogDate(activity.createdAt)} ($activityCreatedAt). In range: $isInRange")
                    isInRange
                }
                Log.d("ActivityRepo", "getActivitiesForTodayFlow: After date filtering, ${filteredActivities.size} activities remain for user $currentUserId.")
                filteredActivities
            }
            .onEach { finalActivities ->
                Log.i("ActivityRepo", "getActivitiesForTodayFlow: 최종적으로 UI로 전달될 활동 개수: ${finalActivities.size} for user $currentUserId.")
                finalActivities.take(3).forEach { act ->
                    Log.i("ActivityRepo", "Final item: Name='${act.name}', CreatedAt=${formatLogDate(act.createdAt)}")
                }
            }
    }

    override suspend fun getActivityById(activityId: String): ActivityItem? {
        // Пытаемся сначала найти по firebaseId (если activityId - это firebaseId)
        var activity = activityDao.getActivityByFirebaseId(activityId)
        if (activity == null) {
            // Если не нашли, и если activityId может быть локальным Long ID, конвертируем и ищем
            activityId.toLongOrNull()?.let { localId ->
                activity = activityDao.getActivityByLocalId(localId)
            }
        }
        Log.d("ActivityRepo", "getActivityById for ID: $activityId, found: ${activity != null}")
        return activity
        // TODO: Добавить логику получения из Firestore, если в локальной БД нет, и последующее сохранение локально.
    }

    override suspend fun deleteActivity(activityId: String) {
        try {
            // Сначала получаем объект, чтобы знать userId и firebaseId для удаления из Firestore
            val activityToDelete = getActivityById(activityId)

            // Удаляем из Room по activityId (который может быть firebaseId или локальным)
            activityDao.deleteActivity(activityId)
            Log.d("ActivityRepo", "Activity deleted locally with ID criteria: $activityId")

            if (activityToDelete != null && activityToDelete.userId.isNotBlank() && activityToDelete.firebaseId != null && activityToDelete.firebaseId.isNotBlank()) {
                firestore.collection("users")
                    .document(activityToDelete.userId)
                    .collection("activities")
                    .document(activityToDelete.firebaseId)
                    .delete()
                    .await()
                Log.d("ActivityRepo", "Activity deleted from Firestore with FirebaseID: ${activityToDelete.firebaseId}")
            } else {
                Log.w("ActivityRepo", "Could not delete from Firestore: missing IDs or activity not found for ID criteria $activityId")
            }
        } catch (e: Exception) {
            Log.e("ActivityRepo", "Error deleting activity for ID criteria: $activityId", e)
            throw e
        }
    }

    private fun formatLogDate(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US)
        return sdf.format(date)
    }
}