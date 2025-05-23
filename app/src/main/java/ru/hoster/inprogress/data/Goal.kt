package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import ru.hoster.inprogress.data.Converters // Assuming you'll create a Converters class
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class GoalType {
    TIME_PER_PERIOD, // e.g., X hours in Y days/week/month
    CONSECUTIVE_DAYS // e.g., work on a task for X days in a row
}

@Entity(tableName = "goals")
@TypeConverters(Converters::class) // For Room to handle custom types like Enum or Date
data class Goal(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    var firebaseId: String? = null,
    var userId: String, // Non-nullable, no default in primary constructor
    var title: String,  // Non-nullable, no default in primary constructor
    var description: String?,
    var type: GoalType, // Non-nullable, no default in primary constructor
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
