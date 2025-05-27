package ru.hoster.inprogress.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.hoster.inprogress.data.TimerSession
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSessionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /** наблюдаем все сессии в коллекции sessions под конкретной задачей */
    fun getSessionsFlow(userId: String, activityFirebaseId: String): Flow<List<TimerSession>> = callbackFlow {
        Log.d("FirestoreSessionRepo", "getSessionsFlow: UserID='$userId', ForFirebaseActivityID='$activityFirebaseId'")
        val listenerRegistration = firestore
            .collection("users").document(userId)
            .collection("activities").document(activityFirebaseId) // <-- ИСПОЛЬЗУЕМ ПРАВИЛЬНЫЙ FIREBASE_ID
            .collection("sessions").orderBy("startTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FirestoreSessionRepo", "getSessionsFlow listen error", e)
                    close(e) // Закрыть Flow с ошибкой
                    return@addSnapshotListener
                }
                val sessions = snapshots?.mapNotNull { doc ->
                    try {
                        // Преобразование документа Firestore в TimerSession
                        // Важно: TimerSession должен иметь конструктор без аргументов или поля по умолчанию для toObject()
                        // Или вы мапите поля вручную.
                        val firestoreSession = doc.toObject<FirestoreSessionData>() // Промежуточный data class
                        TimerSession(
                            id = 0L, // Firestore ID не всегда нужен здесь, или можно взять doc.id
                            activityId = firestoreSession.localActivityId ?: 0L, // Если сохраняли localActivityId
                            userId = userId, // userId из параметра, т.к. он не хранится в документе сессии
                            startTime = firestoreSession.startTime ?: Date(0),
                            endTime = firestoreSession.endTime
                        )
                    } catch (parseEx: Exception) {
                        Log.e("FirestoreSessionRepo", "Error parsing session doc ${doc.id}", parseEx)
                        null
                    }
                } ?: emptyList()
                Log.d("FirestoreSessionRepo", "getSessionsFlow emitting ${sessions.size} sessions.")
                trySend(sessions).isSuccess
            }
        awaitClose {
            Log.d("FirestoreSessionRepo", "getSessionsFlow flow closing, removing listener.")
            listenerRegistration.remove()
        }
    }

    /**
     * Добавляет новую сессию таймера в Firestore.
     * activityId из объекта сессии используется для определения пути.
     */
    suspend fun addSession(userId: String, activityFirebaseId: String, session: TimerSession) {
        val sessionData = hashMapOf(
            "startTime" to session.startTime,
            "endTime" to session.endTime,
            "localActivityId" to session.activityId, // Опционально, для отладки или связи
            "localSessionId" to session.id // Опционально
        )
        Log.d("FirestoreSessionRepo", "addSession: UserID='$userId', ForFirebaseActivityID='$activityFirebaseId', SessionStartTime=${session.startTime}")

        firestore
            .collection("users")
            .document(userId)
            .collection("activities")
            .document(activityFirebaseId) // <-- ИСПОЛЬЗУЕМ ПРАВИЛЬНЫЙ FIREBASE_ID
            .collection("sessions")
            .add(sessionData) // Firestore сгенерирует ID для документа сессии
            .await()
        Log.i("FirestoreSessionRepo", "addSession: Session added to Firestore under users/$userId/activities/$activityFirebaseId/sessions")
    }

    /**
     * Обновляет endTime конкретной сессии таймера в Firestore.
     * Сессия идентифицируется по userId, activityId и её startTime.
     */
    suspend fun updateSessionEndTime(userId: String, activityFirebaseId: String, sessionStartTime: Date, newEndTime: Date) {
        Log.d("FirestoreSessionRepo", "updateSessionEndTime: UserID='$userId', ForFirebaseActivityID='$activityFirebaseId', SessionStartTime=$sessionStartTime")
        val sessionsRef = firestore
            .collection("users")
            .document(userId)
            .collection("activities")
            .document(activityFirebaseId) // <-- ИСПОЛЬЗUЕМ ПРАВИЛЬНЫЙ FIREBASE_ID
            .collection("sessions")

        // Находим сессию по времени начала (предполагая, что startTime уникально для activity/user)
        val querySnapshot = sessionsRef.whereEqualTo("startTime", sessionStartTime).limit(1).get().await()

        if (!querySnapshot.isEmpty) {
            val documentId = querySnapshot.documents[0].id
            sessionsRef.document(documentId).update("endTime", newEndTime).await()
            Log.i("FirestoreSessionRepo", "updateSessionEndTime: Session $documentId updated in Firestore.")
        } else {
            Log.w("FirestoreSessionRepo", "updateSessionEndTime: No session found with startTime $sessionStartTime to update for FirebaseActivityID '$activityFirebaseId'.")
        }
    }
}

data class FirestoreSessionData(
    val startTime: Date? = null,
    val endTime: Date? = null,
    val localActivityId: Long? = null
    // добавьте другие поля, если они есть в Firestore
)