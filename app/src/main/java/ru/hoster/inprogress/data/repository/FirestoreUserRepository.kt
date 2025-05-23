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
import ru.hoster.inprogress.domain.model.Result // Ваш Result класс
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class FirestoreUserRepository @Inject constructor() : UserRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val usersCollection = db.collection("users")
    override suspend fun getUserById(userId: String): Result<UserData?> {
        TODO("Not yet getUserById")
    }

    override suspend fun getUsersByIds(userIds: List<String>): Result<List<UserData>> {
        TODO("Not yet implemented")
    }

    override suspend fun createUserProfile(user: UserData): Result<Unit> {
        return try {
            // Используем userId как ID документа в Firestore для легкого доступа
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
                    close(error) // Закрываем flow с ошибкой
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject<UserData>()).isSuccess
                } else {
                    trySend(null).isSuccess // Пользователь не найден или документ пуст
                }
            }
        awaitClose { listenerRegistration.remove() }
    }
}