package ru.hoster.inprogress.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.hoster.inprogress.data.ActivityItem

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityItem): Long

    @Update
    suspend fun updateActivity(activity: ActivityItem)

    @Delete
    suspend fun deleteActivity(activity: ActivityItem)



    @Query("SELECT * FROM activity_items WHERE firebaseId = :firebaseId LIMIT 1")
    suspend fun getActivityByFirebaseId(firebaseId: String): ActivityItem?

    @Query("SELECT * FROM activity_items WHERE firebaseId = :fid LIMIT 1")
    suspend fun getByFirebaseId(fid: String): ActivityItem?


    @Query("SELECT * FROM activity_items WHERE userId = :userId ORDER BY createdAt DESC")
    fun getActivitiesForUserFlow(userId: String): Flow<List<ActivityItem>>


    @Query("SELECT * FROM activity_items WHERE userId = :userId ORDER BY name ASC")
    fun getAllActivitiesFlowForUser(userId: String): Flow<List<ActivityItem>>

    @Query("SELECT * FROM activity_items WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAllActivitiesList(userId: String): List<ActivityItem>

    @Query("SELECT * FROM activity_items WHERE id = :id")
    suspend fun getActivityByLocalId(id: Long): ActivityItem?
    }