package ru.hoster.inprogress.data.fake

import kotlinx.coroutines.flow.Flow
import ru.hoster.inprogress.domain.model.AuthService
import ru.hoster.inprogress.domain.model.Result

class FakeAuthService(private var currentUserId: String? = "Hoster177_fake_id") : AuthService {

    override fun getCurrentUserId(): String? {
        return currentUserId
    }

    override fun isUserLoggedIn(): Flow<Boolean> {
        TODO("FakeAuth")
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        TODO("FakeAuth")
    }

    override suspend fun signUp(email: String, password: String): Result<String?> {
        TODO("FakeAuth")
    }

    override suspend fun signOut(): Result<Unit> {
        TODO("FakeAuth")
    }

    override suspend fun getCurrentUserEmail(): String? {
        TODO("Not yet implemented")
    }


    fun setCurrentUserId(userId: String?) {
        this.currentUserId = userId
    }
}
