package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    var uid: String = "",

    var email: String? = null,
    var displayName: String? = null,
    var avatarUrl: String? = null,
    var status: String? = null,

    var xp: Long = 0L,
    var level: Int = 1,

    @ServerTimestamp
    var lastLogin: Date? = null,
    var consecutiveLoginDays: Int = 0,

    @ServerTimestamp
    var createdAt: Date? = null,

    var appLanguage: String = "ru"
) {
    constructor() : this("", null, null, null, null, 0L, 1, null, 0, null, "ru")
}
