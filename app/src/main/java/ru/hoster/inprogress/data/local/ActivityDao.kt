package ru.hoster.inprogress.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.hoster.inprogress.data.ActivityItem // Убедись, что импорт правильный

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceActivity(activity: ActivityItem): Long // Возвращает rowId или id

    @Update
    suspend fun updateActivity(activity: ActivityItem)

    @Delete
    suspend fun deleteActivity(activity: ActivityItem)

    @Query("DELETE FROM activity_items WHERE id = :activityId")
    suspend fun deleteActivityById(activityId: Long)

    @Query("DELETE FROM activity_items WHERE firebaseId = :firebaseId")
    suspend fun deleteActivityByFirebaseId(firebaseId: String)

    @Query("SELECT * FROM activity_items WHERE id = :activityId")
    suspend fun getActivityById(activityId: Long): ActivityItem?

    @Query("SELECT * FROM activity_items WHERE firebaseId = :firebaseId")
    suspend fun getActivityByFirebaseId(firebaseId: String): ActivityItem?

    /**
     * Получает поток активностей, созданных или актуальных для указанного диапазона дат (например, за один день).
     * Предполагается, что поле 'createdAt' используется для определения "сегодняшних" задач,
     * или это поле отражает дату, к которой задача привязана.
     *
     * @param startOfDayMillis Начало дня в миллисекундах (UTC).
     * @param endOfDayMillis Конец дня в миллисекундах (UTC) (не включая эту миллисекунду).
     */
    @Query("SELECT * FROM activity_items WHERE userId = :userId AND createdAt >= :startOfDayMillis AND createdAt < :endOfDayMillis ORDER BY createdAt DESC")
    fun getActivitiesForUserAndDateRangeFlow(userId: String, startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<ActivityItem>>

    // Если нужен просто поток всех активностей для пользователя (для отладки или других экранов)
    @Query("SELECT * FROM activity_items WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllActivitiesForUserFlow(userId: String): Flow<List<ActivityItem>>

    // Более специфичные методы обновления, если полная операция updateActivity нежелательна
    // Могут быть полезны для TimerService, чтобы избежать гонки состояний при обновлении только времени

    @Query("UPDATE activity_items SET totalDurationMillisToday = :durationMillis WHERE id = :activityId")
    suspend fun updateDurationForActivity(activityId: Long, durationMillis: Long)

    @Query("UPDATE activity_items SET totalDurationMillisToday = :durationMillis WHERE firebaseId = :firebaseId")
    suspend fun updateDurationForActivityByFirebaseId(firebaseId: String, durationMillis: Long)

    @Query("UPDATE activity_items SET isActive = :isActive WHERE id = :activityId")
    suspend fun updateActiveStateForActivity(activityId: Long, isActive: Boolean)

    @Query("UPDATE activity_items SET isActive = :isActive WHERE firebaseId = :firebaseId")
    suspend fun updateActiveStateForActivityByFirebaseId(firebaseId: String, isActive: Boolean)
}
