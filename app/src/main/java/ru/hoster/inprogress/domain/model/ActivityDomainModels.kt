package ru.hoster.inprogress.domain.model
import kotlinx.coroutines.flow.Flow
import ru.hoster.inprogress.data.ActivityItem
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
    fun getActivitiesForTodayFlow(): Flow<List<ActivityItem>>
    suspend fun getActivityById(id: String): ActivityItem?
    suspend fun deleteActivity(id: String)
    suspend fun updateActivity(activity: ActivityItem) // Для обновления isActive и т.д.
    suspend fun addActivity(activity: ActivityItem)


}
