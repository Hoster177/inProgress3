package ru.hoster.inprogress.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ru.hoster.inprogress.data.ActivityItem
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreActivityRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getActivitiesFlow(userId: String): Flow<List<ActivityItem>> = callbackFlow {
        val ref = firestore
            .collection("users")
            .document(userId)
            .collection("activities")

        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val items = snap?.documents
                ?.mapNotNull { doc ->
                    val uid = doc.getString("userId") ?: return@mapNotNull null
                    val name = doc.getString("name") ?: return@mapNotNull null
                    ActivityItem(
                        id = 0L,
                        firebaseId = doc.id,
                        userId = uid,
                        name = name,
                        totalDurationMillisToday = doc.getLong("totalDurationMillisToday") ?: 0L,
                        isActive = doc.getBoolean("isActive") ?: false,
                        colorHex = doc.getString("colorHex"),
                        createdAt = doc.getDate("createdAt") ?: Date()
                    )
                }
            trySend(items ?: emptyList())
        }
        awaitClose { listener.remove() }
    }
}