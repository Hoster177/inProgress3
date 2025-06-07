package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.ForeignKey
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

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
    var achievementId: String,

    @ServerTimestamp
    var unlockedAt: Date? = null,
    var progress: Int = 0,
    var target: Int = 1
) {
    constructor() : this("", "", null, 0, 1)
}
