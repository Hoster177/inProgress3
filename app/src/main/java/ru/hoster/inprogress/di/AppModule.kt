package ru.hoster.inprogress.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ru.hoster.inprogress.data.repository.FirestoreGroupRepository
import ru.hoster.inprogress.domain.model.AuthService
import ru.hoster.inprogress.domain.model.GroupRepository
import ru.hoster.inprogress.domain.model.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.hoster.inprogress.data.repository.FirestoreActivityRepository
import ru.hoster.inprogress.data.service.FirebaseAuthService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideAuthService(
        userRepository: UserRepository
    ): AuthService {
        return FirebaseAuthService(userRepository)
    }



    @Provides
    @Singleton
    fun provideGroupRepository(firestore: FirebaseFirestore): GroupRepository {
        return FirestoreGroupRepository(firestore)
    }

    @Provides
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideActivityRepository(firestore: FirebaseFirestore): FirestoreActivityRepository {
        return FirestoreActivityRepository(firestore)
    }

}