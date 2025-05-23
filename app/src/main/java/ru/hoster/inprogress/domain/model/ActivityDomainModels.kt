package ru.hoster.inprogress.domain.model
import java.util.Date

// Represents your actual ActivityItem data model from Firebase/Room
// This should align with the ActivityItem model we used in MainScreen.kt
data class ActivityData(
    val id: String = "", // Use String for Firebase ID consistency
    val localId: Long = 0L, // For Room's autoGenerate
    val userId: String = "",
    val name: String = "Unnamed Activity",
    val colorHex: String? = null,
    val totalDurationMillisToday: Long = 0L, // May not be stored directly, but calculated
    val isActive: Boolean = false, // Represents if a timer is currently running for it
    val createdAt: Date = Date(),
    val lastStartTime: Long? = null // To track ongoing timer
)

interface ActivityRepository {
    suspend fun getActivityById(activityId: String): Result<ActivityData?>
    suspend fun insertActivity(activity: ActivityData): Result<String> // Returns ID of new activity
    suspend fun updateActivity(activity: ActivityData): Result<Unit>
    // Potentially: suspend fun deleteActivity(activityId: String): Result<Unit>
}
