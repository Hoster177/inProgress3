package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Entity(tableName = "activities")
data class ActivityItem(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L, // Local DB primary key

    var firebaseId: String? = null, // Firestore document ID
    var userId: String, // Firebase User UID

    var name: String,
    var colorHex: String? = null, // Optional: for UI differentiation

    @ServerTimestamp // Automatically set by Firestore on creation (server-side)
    var createdAt: Date? = null, // When the activity type was first created by the user

    // For tracking specific sessions of this activity
    // This might be better modeled as a separate 'ActivitySession' entity if complex,
    // but for simplicity, we can track the current/last session here.
    var currentSessionStartTime: Date? = null,
    var lastSessionDurationMillis: Long = 0L, // Duration of the most recent completed session

    var totalDurationMillisToday: Long = 0L, // Accumulated duration for this activity today
    var totalDurationMillisAllTime: Long = 0L, // Accumulated duration for this activity ever

    var isActive: Boolean = false, // Is this activity's timer currently running?
    var lastStartedDate: Date? = null // The date this activity was last started (for daily tracking)
) {
    // No-argument constructor for Firebase deserialization
    constructor() : this(0L, null, "", "", null, null, null, 0L, 0L, 0L, false, null)
}