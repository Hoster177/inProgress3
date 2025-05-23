package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey // UID from Firebase Auth will be the primary key
    var uid: String = "",

    var email: String? = null,
    var displayName: String? = null,
    var avatarUrl: String? = null,
    var status: String? = null, // Max 300 chars

    var xp: Long = 0L,
    var level: Int = 1,

    @ServerTimestamp
    var lastLogin: Date? = null,
    var consecutiveLoginDays: Int = 0,

    @ServerTimestamp
    var createdAt: Date? = null, // When the profile was first created in your DB

    var appLanguage: String = "ru" // Default language 'ru' or 'en'
) {
    // No-argument constructor for Firebase
    constructor() : this("", null, null, null, null, 0L, 1, null, 0, null, "ru")
}
