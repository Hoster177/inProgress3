package ru.hoster.inprogress.domain.model // или ваш пакет

import kotlinx.coroutines.flow.Flow

interface AuthService {

    fun getCurrentUserId(): String?
    fun isUserLoggedIn(): Flow<Boolean> // Поток для отслеживания состояния входа
    suspend fun signIn(email: String, password: String): Result<Unit> // Result для обработки успеха/ошибки
    suspend fun signUp(email: String, password: String): Result<String?> // Result<UserId> при успехе
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUserEmail(): String?
    // Можно добавить метод для сброса пароля, если нужно
    // suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}