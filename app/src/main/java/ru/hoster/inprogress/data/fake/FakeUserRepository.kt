package ru.hoster.inprogress.data.fake

import ru.hoster.inprogress.domain.model.UserData
import ru.hoster.inprogress.domain.model.UserRepository
import ru.hoster.inprogress.domain.model.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

class FakeUserRepository : UserRepository {

    private val users = mutableMapOf<String, UserData>()

    init {

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
        delay(300)
        val foundUsers = userIds.mapNotNull { users[it] }
        return if (foundUsers.isNotEmpty() || userIds.isEmpty()) {
            Result.Success(foundUsers)
        } else {
            Result.Success(emptyList())
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
