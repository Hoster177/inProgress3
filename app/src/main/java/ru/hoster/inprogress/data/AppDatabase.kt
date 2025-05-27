package ru.hoster.inprogress.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.hoster.inprogress.data.local.ActivityDao
import ru.hoster.inprogress.data.local.TimerSessionDao

@Database(
    entities = [
        ActivityItem::class,
        TimerSession::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao
    abstract fun timerSessionDao(): TimerSessionDao
    // abstract fun goalDao(): GoalDao // Если есть
}
