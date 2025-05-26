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
import java.util.Calendar // Для фильтрации по дате

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val activityDao: ActivityDao,
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
        // Предполагаем, что authService.getCurrentUserId() доступен или ты его получаешь другим способом
        // Здесь нужен userId для фильтрации. Для примера, я захардкожу, но ты должен получать его динамически.
        // val currentUserId = authService.getCurrentUserId() ?: return flowOf(emptyList())
        // Для демонстрации без authService:
        // return activityDao.getActivitiesForUserFlow("SOME_USER_ID_HERE") // ЗАМЕНИ НА РЕАЛЬНЫЙ ID
        // Более корректная фильтрация по дате для "сегодня":
        return activityDao.getActivitiesForUserFlow("SOME_USER_ID_HERE") // Замени на реальный userId
            .map { activities ->
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val todayEnd = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                activities.filter { it.createdAt.time in todayStart..todayEnd }
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
}