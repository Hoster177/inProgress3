package ru.hoster.inprogress.domain.model
import kotlinx.coroutines.flow.Flow
import ru.hoster.inprogress.data.ActivityItem
import ru.hoster.inprogress.data.TimerSession
import java.util.Date

data class ActivityData(
    val id: String = "",
    val localId: Long = 0L,
    val userId: String = "",
    val name: String = "Unnamed Activity",
    val colorHex: String? = null,
    val totalDurationMillisToday: Long = 0L,
    val isActive: Boolean = false,
    val createdAt: Date = Date(),
    val lastStartTime: Long? = null
)

interface ActivityRepository {
    fun getEnrichedActivitiesFlow(): Flow<List<ActivityItem>>
    suspend fun getActivityById(id: String): ActivityItem?
    suspend fun deleteActivity(id: String)
    suspend fun updateActivity(activity: ActivityItem)
    suspend fun addActivity(activity: ActivityItem)

    suspend fun startSession(activityId: Long)
    suspend fun stopSession(activityId: Long)
    fun getSessionsForDateRange(userId: String, from: Date, to: Date): Flow<List<TimerSession>>

    suspend fun getAllActivities(userId: String): List<ActivityItem>


}
