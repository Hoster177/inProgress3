package ru.hoster.inprogress.data.repository // или ваш пакет

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.hoster.inprogress.domain.model.UserData
import ru.hoster.inprogress.domain.model.UserRepository
import ru.hoster.inprogress.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class FirestoreUserRepository @Inject constructor() : UserRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val usersCollection = db.collection("users")

    override suspend fun getUserById(userId: String): Result<UserData?> {
        return try {
            val documentSnapshot = usersCollection.document(userId).get().await()
            val user = documentSnapshot.toObject(UserData::class.java)
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getUsersByIds(userIds: List<String>): Result<List<UserData>> {
        if (userIds.isEmpty()) {
            return Result.Success(emptyList())
        }
        return try {
            val querySnapshot = usersCollection.whereIn("userId", userIds).get().await()
            val users = querySnapshot.toObjects(UserData::class.java)
            Result.Success(users)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createUserProfile(user: UserData): Result<Unit> {
        return try {
            usersCollection.document(user.userId).set(user).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreUserRepo", "Error creating user profile", e)
            Result.Error(e)
        }
    }

    suspend fun getUserProfile(userId: String): Result<UserData?> {
        return try {
            val documentSnapshot = usersCollection.document(userId).get().await()
            val userProfile = documentSnapshot.toObject<UserData>()
            Result.Success(userProfile)
        } catch (e: Exception) {
            Log.e("FirestoreUserRepo", "Error getting user profile", e)
            Result.Error(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getUserProfileFlow(userId: String): Flow<UserData?> = callbackFlow {
        val listenerRegistration = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreUserRepo", "Listen failed.", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject<UserData>()).isSuccess
                } else {
                    trySend(null).isSuccess
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun updateUserProfile(user: UserData): Result<Unit> {
        return try {
            if (user.userId.isBlank()) {
                return Result.Error(IllegalArgumentException("User ID cannot be blank for update."))
            }
            usersCollection.document(user.userId).set(user).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    suspend fun updateUserProfile(userId: String, name: String, avatarId: String): Boolean {
        if (userId.isBlank()) {

            return false
        }
        return try {
            val updates = mapOf(
                "displayName" to name,
                "avatarUrl" to avatarId
            )
            usersCollection.document(userId).update(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}