package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.ForeignKey
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// This table links users to the achievements they've unlocked.
@Entity(
    tableName = "user_achievements",
    primaryKeys = ["userId", "achievementId"],
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["uid"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Achievement::class,
            parentColumns = ["id"],
            childColumns = ["achievementId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserAchievement(
    var userId: String,
    var achievementId: String, // Foreign key to Achievement.id

    @ServerTimestamp
    var unlockedAt: Date? = null,
    var progress: Int = 0, // Optional: if achievements have stages or progress
    var target: Int = 1 // Optional: target for progress, default 1 for simple unlock
) {
    // No-argument constructor
    constructor() : this("", "", null, 0, 1)
}
