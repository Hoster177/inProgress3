package ru.hoster.inprogress.data.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.hoster.inprogress.domain.model.AuthService
import ru.hoster.inprogress.domain.model.Result
import ru.hoster.inprogress.domain.model.UserData // Импортируем UserData
import ru.hoster.inprogress.domain.model.UserRepository // Импортируем UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class FirebaseAuthService @Inject constructor(
    private val userRepository: UserRepository // <--- ВНЕДРЯЕМ UserRepository
) : AuthService {

    private val firebaseAuth: FirebaseAuth = Firebase.auth

    // ... getCurrentUserId() и isUserLoggedIn() остаются без изменений ...
    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun isUserLoggedIn(): Flow<Boolean> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null).isSuccess
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }


    override suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuthService", "Sign in failed", e)
            Result.Error(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<String?> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid
            if (userId != null) {
                // Создаем профиль пользователя в Firestore
                val newUser = UserData(
                    userId = userId,
                    email = email,
                    displayName = email.substringBefore('@') // Простой вариант для displayName по умолчанию
                )
                val profileCreationResult = userRepository.createUserProfile(newUser)
                if (profileCreationResult is Result.Error) {
                    // Если создание профиля не удалось, можно вернуть ошибку или залогировать
                    // и продолжить, считая регистрацию в Auth успешной.
                    // Зависит от вашей логики обработки ошибок.
                    Log.e("FirebaseAuthService", "Failed to create user profile in Firestore", profileCreationResult.exception)
                    // Можно вернуть Result.Error(profileCreationResult.exception) если это критично
                }
                Result.Success(userId)
            } else {
                Result.Error(Exception("Firebase Auth user creation failed, user or UID is null."))
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuthService", "Sign up failed", e)
            Result.Error(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuthService", "Sign out failed", e)
            Result.Error(e)
        }
    }
}