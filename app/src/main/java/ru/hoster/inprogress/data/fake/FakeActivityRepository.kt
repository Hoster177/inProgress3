package ru.hoster.inprogress.data.fake
import ru.hoster.inprogress.domain.model.ActivityData
import ru.hoster.inprogress.domain.model.ActivityRepository
import ru.hoster.inprogress.domain.model.Result
import kotlinx.coroutines.delay
import java.util.Date
import java.util.UUID

class FakeActivityRepository : ActivityRepository {

    private val activities = mutableMapOf<String, ActivityData>()
    private var localIdCounter = 1L

    init {
        // Pre-populate with some fake activities
        val activity1Id = "activity_id_fake_1"
        val activity2Id = "activity_id_fake_2"
        val currentUser = "Hoster177_fake_id" // Assuming this aligns with FakeAuthService

        addActivity(
            ActivityData(
                id = activity1Id,
                localId = localIdCounter++,
                userId = currentUser,
                name = "Разработка UI (Fake)",
                colorHex = "#FF6B6B",
                createdAt = Date(System.currentTimeMillis() - 1000 * 60 * 60 * 3) // 3 hours ago
            )
        )
        addActivity(
            ActivityData(
                id = activity2Id,
                localId = localIdCounter++,
                userId = currentUser,
                name = "Встреча с командой (Fake)",
                colorHex = "#4ECDC4",
                createdAt = Date(System.currentTimeMillis() - 1000 * 60 * 60 * 1) // 1 hour ago
            )
        )
    }

    fun addActivity(activity: ActivityData) {
        activities[activity.id] = activity
    }

    override suspend fun getActivityById(activityId: String): Result<ActivityData?> {
        delay(200) // Simulate network delay
        val activity = activities[activityId]
        return if (activity != null) {
            Result.Success(activity)
        } else {
            // Result.Success(null) // To indicate activity not found but no system error
            Result.Error(Exception("FakeActivityRepo: Activity with ID $activityId not found."))
        }
    }

    override suspend fun insertActivity(activity: ActivityData): Result<String> {
        delay(400)
        val newId = activity.id.ifEmpty { UUID.randomUUID().toString() }
        val newLocalId = if(activity.localId == 0L) localIdCounter++ else activity.localId
        val activityToSave = activity.copy(id = newId, localId = newLocalId)
        activities[newId] = activityToSave
        return Result.Success(newId)
    }

    override suspend fun updateActivity(activity: ActivityData): Result<Unit> {
        delay(400)
        if (!activities.containsKey(activity.id)) {
            return Result.Error(Exception("FakeActivityRepo: Activity with ID ${activity.id} not found for update."))
        }
        activities[activity.id] = activity
        return Result.Success(Unit)
    }

    fun clearActivities() {
        activities.clear()
        localIdCounter = 1L
    }
}
