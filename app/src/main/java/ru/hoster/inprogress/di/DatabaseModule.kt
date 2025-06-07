package ru.hoster.inprogress.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.hoster.inprogress.data.local.ActivityDao
import ru.hoster.inprogress.data.AppDatabase
import ru.hoster.inprogress.data.local.TimerSessionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "inprogress_database"
        )

            .build()
    }

    @Provides
    @Singleton
    fun provideActivityDao(appDatabase: AppDatabase): ActivityDao {
        return appDatabase.activityDao()
    }

    @Provides
    @Singleton
    fun provideTimerSessionDao(appDatabase: AppDatabase): TimerSessionDao {
        return appDatabase.timerSessionDao()
    }


}