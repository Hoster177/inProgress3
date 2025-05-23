package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// This class defines the types of achievements available in the app.
// It's likely to be prepopulated or managed remotely rather than user-created.
@Entity(tableName = "achievements_definitions")
data class Achievement(
    @PrimaryKey
    var id: String, // e.g., "LOGIN_STREAK_7_DAYS", "100_HOURS_TRACKED"

    var name: String,
    var description: String,
    var iconRef: String?, // Reference to a drawable resource or a URL
    var xpReward: Long = 0L
) {
    // No-argument constructor
    constructor() : this("", "", "", null, 0L)
}