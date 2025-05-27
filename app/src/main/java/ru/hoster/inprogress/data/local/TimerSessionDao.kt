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

    /** получить все сессии пользователя, отсортированные по startTime */
    @Query("SELECT * FROM timer_sessions WHERE userId = :uid ORDER BY startTime DESC")
    fun getAllSessionsFlow(uid: String): Flow<List<TimerSession>>

    /** получить сессии в диапазоне дат (inclusive) */
    @Query("""
    SELECT * FROM timer_sessions
     WHERE userId = :uid
       AND date(startTime/1000, 'unixepoch') BETWEEN date(:from/1000,'unixepoch') AND date(:to/1000,'unixepoch')
     ORDER BY startTime ASC
  """)
    fun getSessionsForDateRangeFlow(uid: String, from: Date, to: Date): Flow<List<TimerSession>>

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
}