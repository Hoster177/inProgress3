package ru.hoster.inprogress.domain.model

import kotlinx.coroutines.flow.Flow


sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: Exception) : Result<Nothing>()
}

interface UserRepository {
    suspend fun getUserById(userId: String): Result<UserData?>
    suspend fun getUsersByIds(userIds: List<String>): Result<List<UserData>>
    suspend fun createUserProfile(user: UserData): Result<Unit>
    fun getUserProfileFlow(userId: String): Flow<UserData?>
    suspend fun updateUserProfile(user: UserData): Result<Unit>
}