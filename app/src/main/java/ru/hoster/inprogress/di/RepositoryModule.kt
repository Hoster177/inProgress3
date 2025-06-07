package ru.hoster.inprogress.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.hoster.inprogress.data.repository.ActivityRepositoryImpl
import ru.hoster.inprogress.data.repository.FirestoreUserRepository
import ru.hoster.inprogress.data.repository.GoalRepositoryImpl
import ru.hoster.inprogress.domain.model.ActivityRepository
import ru.hoster.inprogress.domain.model.GoalRepository
import ru.hoster.inprogress.domain.model.UserRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        firestoreUserRepository: FirestoreUserRepository
    ): UserRepository


    @Binds
    @Singleton
    abstract fun bindActivityRepository(
        activityRepositoryImpl: ActivityRepositoryImpl
    ): ActivityRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(
        goalRepositoryImpl: GoalRepositoryImpl
    ): GoalRepository



}