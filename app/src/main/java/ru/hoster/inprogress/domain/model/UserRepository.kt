package ru.hoster.inprogress.domain.model

import kotlinx.coroutines.flow.Flow

// Импортируем Result, если он определен в этом же пакете или в другом общем
// Если Result определен в другом месте, например, ru.hoster.inprogress.domain.util.Result,
// то используйте этот импорт. Для примера, предположим, он здесь же.

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>() // Обрати внимание, Success здесь generic
    data class Error(val message: Exception) : Result<Nothing>()
}

interface UserRepository {
    suspend fun getUserById(userId: String): Result<UserData?>
    suspend fun getUsersByIds(userIds: List<String>): Result<List<UserData>>
    suspend fun createUserProfile(user: UserData): Result<Unit>
    fun getUserProfileFlow(userId: String): Flow<UserData?> // Если нужна реактивность
    // Можно добавить другие методы:
    // suspend fun updateUserProfile(user: UserData): Result<Unit>
}