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
}
