package ru.hoster.inprogress.navigation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.hoster.inprogress.data.ActivityItem
import ru.hoster.inprogress.data.TimerSession
import ru.hoster.inprogress.domain.model.ActivityRepository
import ru.hoster.inprogress.domain.model.AuthService
import ru.hoster.inprogress.data.local.TimerSessionDao
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// Data class для отображения статистики по одной активности
data class ActivityStatsItem(
    val activityId: Long,
    val activityName: String,
    val totalDurationMillis: Long,
    val totalDurationFormatted: String,
    val colorHex: String? // Цвет активности для UI
)

// Data class для отображения одной сессии в списке
data class SessionDisplayItem(
    val activityName: String,
    val startTimeFormatted: String,
    val endTimeFormatted: String,
    val durationFormatted: String,
    val originalSession: TimerSession // Для возможной детализации или редактирования
)

// Renamed from StatsScreenUiState to DailyStatsUiState
data class DailyStatsUiState(
    val selectedDate: Date = Date(), // Сегодня по умолчанию
    val selectedDateFormatted: String = formatDateForDisplay(Date()),
    val sessionsForSelectedDate: List<SessionDisplayItem> = emptyList(),
    val activitySummaryForSelectedDate: List<ActivityStatsItem> = emptyList(),
    val totalTimeForSelectedDateFormatted: String = formatDurationViewModel(0L, forceHours = true),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val allActivities: List<ActivityItem> = emptyList() // Для маппинга ID в имя
)

// Helper function to format duration, added here for completeness


fun formatDateForDisplay(date: Date): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
    return sdf.format(date)
}

fun formatTimeForSessionList(date: Date?): String {
    if (date == null) return "--:--"
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(date)
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val timerSessionDao: TimerSessionDao,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyStatsUiState(isLoading = true))
    val uiState: StateFlow<DailyStatsUiState> = _uiState.asStateFlow()

    private val selectedDateFlow = MutableStateFlow(getStartOfDay(Date()))

    init {
        viewModelScope.launch {
            val userId = authService.getCurrentUserId()
            if (userId == null) {
                _uiState.value = DailyStatsUiState(isLoading = false, errorMessage = "Пользователь не найден")
                return@launch
            }

            try {
                val activities = activityRepository.getAllActivities(userId) // Assuming this is a suspend function
                _uiState.update { it.copy(allActivities = activities) }
            } catch (e: Exception) {
                Log.e("StatsVM", "Error loading all activities", e)
                // Consider updating UI state with an error message or handling this more gracefully
                // For now, if this fails, activity names might be "Неизвестная активность"
            }

            selectedDateFlow
                .onEach { date -> _uiState.update { it.copy(isLoading = true, selectedDate = date, selectedDateFormatted = formatDateForDisplay(date)) } }
                .flatMapLatest { date ->
                    val fromDate = getStartOfDay(date)
                    val toDate = getEndOfDay(date)
                    Log.d("StatsVM", "Loading stats for date: $fromDate to $toDate for user $userId")
                    timerSessionDao.getSessionsForDateRangeFlow(userId, fromDate, toDate)
                }
                .catch { e ->
                    Log.e("StatsVM", "Error loading sessions for date range", e)
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка загрузки сессий: ${e.message}") }
                }
                .onEach { sessions ->
                    Log.d("StatsVM", "Received ${sessions.size} sessions for selected date.")
                    processSessionsForUi(sessions)
                    // isLoading is set to false inside processSessionsForUi or after it
                }
                .launchIn(viewModelScope)
        }
    }

    fun selectDate(date: Date) {
        selectedDateFlow.value = getStartOfDay(date)
    }

    private fun processSessionsForUi(sessions: List<TimerSession>) {
        val currentAllActivities = _uiState.value.allActivities

        val displaySessions = sessions.map { session ->
            val activityName = currentAllActivities.find { it.id == session.activityId }?.name ?: "Неизвестная активность"
            val durationMillis = if (session.endTime != null && session.startTime != null) {
                session.endTime.time - session.startTime.time
            } else {
                0L // Or handle ongoing sessions differently if they can appear here
            }
            SessionDisplayItem(
                activityName = activityName,
                startTimeFormatted = formatTimeForSessionList(session.startTime),
                endTimeFormatted = formatTimeForSessionList(session.endTime),
                durationFormatted = formatDurationViewModel(durationMillis),
                originalSession = session
            )
        }

        val activitySummary = sessions
            .filter { it.endTime != null } // Consider only completed sessions for summary
            .groupBy { it.activityId }
            .mapNotNull { (activityId, sessionsForActivity) ->
                val activity = currentAllActivities.find { it.id == activityId }
                if (activity != null) {
                    val totalMillis = sessionsForActivity.sumOf { session ->
                        // endTime is guaranteed to be non-null here due to filter
                        session.endTime!!.time - session.startTime.time
                    }
                    ActivityStatsItem(
                        activityId = activity.id,
                        activityName = activity.name,
                        totalDurationMillis = totalMillis,
                        totalDurationFormatted = formatDurationViewModel(totalMillis, forceHours = true),
                        colorHex = activity.colorHex
                    )
                } else null
            }.sortedByDescending { it.totalDurationMillis }

        val totalTimeForDayMillis = activitySummary.sumOf { it.totalDurationMillis }

        _uiState.update {
            it.copy(
                sessionsForSelectedDate = displaySessions,
                activitySummaryForSelectedDate = activitySummary,
                totalTimeForSelectedDateFormatted = formatDurationViewModel(totalTimeForDayMillis, forceHours = true),
                isLoading = false // Set isLoading to false after processing
            )
        }
    }

    private fun getStartOfDay(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun getEndOfDay(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }
}