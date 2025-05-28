package ru.hoster.inprogress.data.fake

import ru.hoster.inprogress.domain.model.UserData
import ru.hoster.inprogress.domain.model.UserRepository
import ru.hoster.inprogress.domain.model.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

class FakeUserRepository : UserRepository {

    private val users = mutableMapOf<String, UserData>()

    init {
        // Pre-populate with some fake users
        addUser(UserData(userId = "Hoster177_fake_id", displayName = "Hoster177 (You)", avatarUrl = null))
        addUser(UserData(userId = "user_admin_123", displayName = "Alice Admin", avatarUrl = "https://example.com/alice.png"))
        addUser(UserData(userId = "user_member_3", displayName = "Bob Member", avatarUrl = null))
        addUser(UserData(userId = "user_member_4", displayName = "Charlie Member", avatarUrl = "https://example.com/charlie.png"))
        addUser(UserData(userId = "user_to_add_5", displayName = "Diana Newbie", avatarUrl = null))
    }

    fun addUser(user: UserData) {
        users[user.userId] = user
    }

    override suspend fun getUserById(userId: String): Result<UserData?> {
        TODO("Not yet getUserById")
    }

    override suspend fun getUsersByIds(userIds: List<String>): Result<List<UserData>> {
        delay(300) // Simulate network delay
        val foundUsers = userIds.mapNotNull { users[it] }
        return if (foundUsers.isNotEmpty() || userIds.isEmpty()) {
            Result.Success(foundUsers)
        } else {
            // If some users were requested but none found (e.g., all IDs were invalid)
            // Depending on strictness, this could be an empty success or an error.
            // For simplicity, returning success with empty list if no specific error condition is met.
            // If you want to return error if ANY id is not found, logic would be more complex.
            Result.Success(emptyList()) // Or Result.Error(Exception("Some users not found"))
        }
    }

    override suspend fun createUserProfile(user: UserData): Result<Unit> {
        TODO("Not yet createUserProfile")
    }

    suspend fun getUserProfile(userId: String): Result<UserData?> {
        TODO("Not yet getUserProfile")
    }

    override fun getUserProfileFlow(userId: String): Flow<UserData?> {
        TODO("Not yet getUserProfileFlow")
    }

    override suspend fun updateUserProfile(user: UserData): Result<Unit> {
        TODO("Not yet implemented")
    }
}
