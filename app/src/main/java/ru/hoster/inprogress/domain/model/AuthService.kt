package ru.hoster.inprogress.domain.model

import kotlinx.coroutines.flow.Flow

interface AuthService {

    fun getCurrentUserId(): String?
    fun isUserLoggedIn(): Flow<Boolean>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<String?>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUserEmail(): String?
}