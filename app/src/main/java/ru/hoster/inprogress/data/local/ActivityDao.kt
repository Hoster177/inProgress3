package ru.hoster.inprogress.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.hoster.inprogress.data.ActivityItem

@Dao
interface ActivityDao {

    // Main insert method. On conflict with PRIMARY KEY (id), it replaces.
    // UNIQUE constraint on firebaseId is handled by repository logic before calling this.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityItem): Long // Returns new rowId or existing id if replaced

    @Update
    suspend fun updateActivity(activity: ActivityItem) // Updates based on Primary Key 'id'

    @Delete
    suspend fun deleteActivity(activity: ActivityItem) // Deletes based on Primary Key 'id'

    // Specific getters
    @Query("SELECT * FROM activity_items WHERE id = :localId LIMIT 1")
    suspend fun getActivityByLocalId(localId: Long): ActivityItem?

    @Query("SELECT * FROM activity_items WHERE firebaseId = :firebaseId LIMIT 1")
    suspend fun getActivityByFirebaseId(firebaseId: String): ActivityItem?

    // Alias for getActivityByFirebaseId for clarity in repository's sync logic
    @Query("SELECT * FROM activity_items WHERE firebaseId = :fid LIMIT 1")
    suspend fun getByFirebaseId(fid: String): ActivityItem?


    // Flows for observing data
    @Query("SELECT * FROM activity_items WHERE userId = :userId ORDER BY createdAt DESC")
    fun getActivitiesForUserFlow(userId: String): Flow<List<ActivityItem>>

    @Query("SELECT * FROM activity_items WHERE userId = :userId ORDER BY name ASC") // Или другая сортировка
    suspend fun getAllActivitiesList(userId: String): List<ActivityItem> // Для однократного получения списка


    // Unused/Duplicate DAO methods to consider removing:
    // @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertOrReplaceActivity(activity: ActivityItem): Long // Duplicate of insertActivity
    // @Query("DELETE FROM activity_items WHERE id = :activityId") suspend fun deleteActivityById(activityId: Long) // Covered by @Delete if you fetch item first
    // @Query("DELETE FROM activity_items WHERE firebaseId = :firebaseId") suspend fun deleteActivityByFirebaseId(firebaseId: String) // Covered by @Delete if you fetch item first
    // @Query("SELECT * FROM activity_items WHERE id = :activityId") suspend fun getActivityById(activityId: Long): ActivityItem? // Duplicate of getActivityByLocalId
    // @Query("DELETE FROM activity_items WHERE id = :activityId OR firebaseId = :activityId") suspend fun deleteActivity(activityId: String) // Risky; use specific deletion after fetching
    // @Query("SELECT * FROM activity_items WHERE userId = :userId AND createdAt >= :startOfDayMillis AND createdAt < :endOfDayMillis ORDER BY createdAt DESC")
    // fun getActivitiesForUserAndDateRangeFlow(userId: String, startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<ActivityItem>> // Can be done by filtering getActivitiesForUserFlow in repo
    // @Query("UPDATE activity_items SET totalDurationMillisToday = :durationMillis WHERE id = :activityId") suspend fun updateDurationForActivity(activityId: Long, durationMillis: Long) // Prefer full updateActivity
    // @Query("UPDATE activity_items SET totalDurationMillisToday = :durationMillis WHERE firebaseId = :firebaseId") suspend fun updateDurationForActivityByFirebaseId(firebaseId: String, durationMillis: Long) // Prefer full updateActivity
    // @Query("UPDATE activity_items SET isActive = :isActive WHERE id = :activityId") suspend fun updateActiveStateForActivity(activityId: Long, isActive: Boolean) // Prefer full updateActivity
    // @Query("UPDATE activity_items SET isActive = :isActive WHERE firebaseId = :firebaseId") suspend fun updateActiveStateForActivityByFirebaseId(firebaseId: String, isActive: Boolean) // Prefer full updateActivity
}