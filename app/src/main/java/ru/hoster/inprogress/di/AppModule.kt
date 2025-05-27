package ru.hoster.inprogress.di // Create a 'di' (dependency injection) package

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ru.hoster.inprogress.data.repository.FirestoreGroupRepository
import ru.hoster.inprogress.data.repository.FirestoreUserRepository
import ru.hoster.inprogress.data.fake.FakeActivityRepository // Import new fake
import ru.hoster.inprogress.data.fake.FakeAuthService
import ru.hoster.inprogress.data.fake.FakeGroupRepository
import ru.hoster.inprogress.data.fake.FakeUserRepository
import ru.hoster.inprogress.domain.model.ActivityRepository // Import new interface
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
import ru.hoster.inprogress.data.repository.remote.FirestoreActivityRepository
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
        userRepository: UserRepository // <--- Hilt предоставит этот параметр из provideUserRepository()
    ): AuthService {
        // Теперь передаем userRepository в конструктор
        return FirebaseAuthService(userRepository) // <--- СТРОКА 43 (примерно) ИСПРАВЛЕНА
    }

    //@Provides
    //@Singleton
    //fun provideUserRepository(): UserRepository { // Inject Firestore
    //    return FirestoreUserRepository() // Provide real implementation
    //}

    @Provides
    @Singleton
    fun provideGroupRepository(firestore: FirebaseFirestore): GroupRepository { // Inject Firestore
        return FirestoreGroupRepository(firestore) // Provide real implementation
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