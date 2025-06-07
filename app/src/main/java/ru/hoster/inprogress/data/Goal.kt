package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import ru.hoster.inprogress.data.Converters
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class GoalType {
    TIME_PER_PERIOD,
    CONSECUTIVE_DAYS
}

@Entity(tableName = "goals")
@TypeConverters(Converters::class)
data class Goal(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    var firebaseId: String? = null,
    var userId: String,
    var title: String,
    var description: String?,
    var type: GoalType,
    var targetDurationMillis: Long? = null,
    var periodDays: Int? = null,
    var targetConsecutiveDays: Int? = null,
    var currentProgressMillis: Long = 0L,
    var currentConsecutiveDays: Int = 0,
    @ServerTimestamp
    var createdAt: Date? = null,
    var deadline: Date? = null,
    var isAchieved: Boolean = false,
    var isDefault: Boolean = false
) {
    constructor() : this(0L, null, "", "", null, GoalType.TIME_PER_PERIOD, null, null, null, 0L, 0, null, null, false, false)
}
