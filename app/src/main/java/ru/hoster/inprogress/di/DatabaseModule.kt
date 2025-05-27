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
@InstallIn(SingletonComponent::class) // Зависимости будут жить пока живо приложение
object DatabaseModule {

    @Provides
    @Singleton // Гарантирует, что будет только один экземпляр AppDatabase
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "inprogress_database"
        )
            // .fallbackToDestructiveMigration() // Для разработки. Удали или замени на миграции для продакшена.
            .build()
    }

    @Provides
    @Singleton // Dao обычно тоже синглтоны, так как они привязаны к единственному экземпляру БД
    fun provideActivityDao(appDatabase: AppDatabase): ActivityDao {
        return appDatabase.activityDao()
    }

    @Provides
    @Singleton // Dao обычно тоже синглтоны, так как они привязаны к единственному экземпляру БД
    fun provideTimerSessionDao(appDatabase: AppDatabase): TimerSessionDao {
        return appDatabase.timerSessionDao()
    }

    // Если у тебя есть GoalDao, добавь аналогичный @Provides метод для него:
    /*
    @Provides
    @Singleton
    fun provideGoalDao(appDatabase: AppDatabase): GoalDao {
        return appDatabase.goalDao()
    }
    */
}