package ru.hoster.inprogress.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.hoster.inprogress.data.TimerSession
import java.util.Date

@Dao
interface TimerSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TimerSession): Long

    @Update
    suspend fun updateSession(session: TimerSession)

    // Получить все сессии для пользователя (для отладки или других нужд)
    @Query("SELECT * FROM timer_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getAllSessionsFlow(userId: String): Flow<List<TimerSession>>



    @Query("SELECT * FROM timer_sessions WHERE userId = :userId AND startTime BETWEEN :fromDate AND :toDate ORDER BY startTime DESC")
    fun getSessionsForDateRangeFlow(userId: String, fromDate: Date, toDate: Date): Flow<List<TimerSession>>


    @Query("SELECT * FROM timer_sessions WHERE userId = :userId AND activityId = :activityId AND endTime IS NULL LIMIT 1")
    suspend fun getActiveSession(userId: String, activityId: Long): TimerSession?


    @Query("SELECT * FROM timer_sessions WHERE userId = :userId AND activityId = :activityId ORDER BY startTime DESC")
    fun getSessionsForActivityFlow(userId: String, activityId: Long): Flow<List<TimerSession>>


    @Query("SELECT * FROM timer_sessions WHERE userId = :userId AND startTime BETWEEN :fromDate AND :toDate ORDER BY startTime DESC")
    suspend fun getSessionsForDateRange(userId: String, fromDate: Date, toDate: Date): List<TimerSession>


}