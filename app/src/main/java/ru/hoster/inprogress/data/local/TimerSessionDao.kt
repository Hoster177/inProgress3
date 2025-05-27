package ru.hoster.inprogress.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.hoster.inprogress.data.TimerSession
import java.util.Date

@Dao
interface TimerSessionDao {
    /** вставить новую сессию (startTime уже проставлен) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TimerSession): Long

    /** обновить endTime для существующей строки */
    @Update
    suspend fun updateSession(session: TimerSession)

    // Получить все сессии для пользователя (для отладки или других нужд)
    @Query("SELECT * FROM timer_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getAllSessionsFlow(userId: String): Flow<List<TimerSession>>


    /** получить сессии в диапазоне дат (inclusive) */

    @Query("SELECT * FROM timer_sessions WHERE userId = :userId AND startTime BETWEEN :fromDate AND :toDate ORDER BY startTime DESC")
    fun getSessionsForDateRangeFlow(userId: String, fromDate: Date, toDate: Date): Flow<List<TimerSession>>

    /**
     * Получить активную сессию (endTime IS NULL) для указанной задачи и пользователя.
     * Предполагается, что для одной задачи у одного пользователя может быть не более одной активной сессии.
     */
    @Query("SELECT * FROM timer_sessions WHERE userId = :userId AND activityId = :activityId AND endTime IS NULL LIMIT 1")
    suspend fun getActiveSession(userId: String, activityId: Long): TimerSession?

    /**
     * Получить все сессии для конкретной задачи пользователя, отсортированные по startTime DESC.
     * Полезно для просмотра истории сессий по одной задаче.
     */
    @Query("SELECT * FROM timer_sessions WHERE userId = :userId AND activityId = :activityId ORDER BY startTime DESC")
    fun getSessionsForActivityFlow(userId: String, activityId: Long): Flow<List<TimerSession>>

    // Если нужен не Flow, а просто suspend функция для однократного получения:
    @Query("SELECT * FROM timer_sessions WHERE userId = :userId AND startTime BETWEEN :fromDate AND :toDate ORDER BY startTime DESC")
    suspend fun getSessionsForDateRange(userId: String, fromDate: Date, toDate: Date): List<TimerSession>
}