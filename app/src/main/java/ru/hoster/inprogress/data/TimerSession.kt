package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "timer_sessions",
    indices = [ Index(value = ["activityId"]), Index(value = ["userId"]) ],
    foreignKeys = [
        ForeignKey(
            entity = ActivityItem::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TimerSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,


    val activityId: Long,

    val userId: String,

    /** время старта сессии */
    val startTime: Date,

    /** время остановки; null, если сессия ещё активна */
    val endTime: Date? = null
)