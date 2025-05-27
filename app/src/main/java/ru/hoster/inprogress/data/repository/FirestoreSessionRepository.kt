package ru.hoster.inprogress.data.repository

import com.google.firebase.firestore.FirebaseFirestore
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
    fun getSessionsFlow(userId: String, activityId: String): Flow<List<TimerSession>> = callbackFlow {
        val ref = firestore
            .collection("users")
            .document(userId)
            .collection("activities")
            .document(activityId) // activityId здесь уже String
            .collection("sessions")
            .orderBy("startTime") // Добавим сортировку для консистентности

        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err); return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { doc ->
                val s = doc.getTimestamp("startTime")?.toDate() ?: return@mapNotNull null
                val e = doc.getTimestamp("endTime")?.toDate()
                TimerSession(
                    id = 0L, // Локальный ID не релевантен для Firestore-сущностей в этом контексте
                    activityId = activityId.toLongOrNull() ?: 0L,
                    userId = userId,
                    startTime = s,
                    endTime = e
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Добавляет новую сессию таймера в Firestore.
     * activityId из объекта сессии используется для определения пути.
     */
    suspend fun addSession(userId: String, session: TimerSession) {
        val sessionData = hashMapOf(
            "startTime" to session.startTime,
            "endTime" to session.endTime, // Будет null для новых сессий
            // userId и activityId являются частью пути документа
        )

        firestore
            .collection("users")
            .document(userId)
            .collection("activities")
            .document(session.activityId.toString()) // activityId в TimerSession - Long
            .collection("sessions")
            .add(sessionData) // .add() создает документ с авто-сгенерированным ID
            .await() // Ожидаем завершения операции
    }

    /**
     * Обновляет endTime конкретной сессии таймера в Firestore.
     * Сессия идентифицируется по userId, activityId и её startTime.
     */
    suspend fun updateSessionEndTime(userId: String, activityId: Long, startTime: Date, newEndTime: Date) {
        val sessionsRef = firestore
            .collection("users")
            .document(userId)
            .collection("activities")
            .document(activityId.toString()) // activityId здесь Long
            .collection("sessions")

        // Ищем сессию с совпадающим startTime
        val querySnapshot = sessionsRef
            .whereEqualTo("startTime", startTime)
            .limit(1) // Ожидаем не более одной активной сессии с таким startTime
            .get()
            .await()

        if (!querySnapshot.isEmpty) {
            val documentToUpdate = querySnapshot.documents.first()
            sessionsRef.document(documentToUpdate.id)
                .update("endTime", newEndTime)
                .await()
        } else {
            // Случай, когда сессия для обновления не найдена в Firestore.
            // Можно логировать или выбрасывать исключение.
            // Пока просто выведем предупреждение.
            System.err.println(
                "FirestoreSync: Could not find session in Firestore to update endTime for " +
                        "userId=$userId, activityId=$activityId, startTime=$startTime"
            )
        }
    }
}