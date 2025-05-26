package ru.hoster.inprogress.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.hoster.inprogress.data.local.ActivityDao

@Database(entities = [ActivityItem::class /*, Goal::class */], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao
    // abstract fun goalDao(): GoalDao // Если есть
/*
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inprogress_database"
                )
                    // .fallbackToDestructiveMigration() // Для простоты на время разработки, удали для продакшена
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }*/
}
