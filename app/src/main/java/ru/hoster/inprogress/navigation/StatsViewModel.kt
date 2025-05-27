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
import ru.hoster.inprogress.service.TimerService // Если будете брать сессии из него
import ru.hoster.inprogress.data.local.TimerSessionDao // Альтернатива: прямой доступ к DAO сессий
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

data class StatsScreenUiState(
    val selectedDate: Date = Date(), // Сегодня по умолчанию
    val selectedDateFormatted: String = formatDateForDisplay(Date()),
    val sessionsForSelectedDate: List<SessionDisplayItem> = emptyList(),
    val activitySummaryForSelectedDate: List<ActivityStatsItem> = emptyList(),
    val totalTimeForSelectedDateFormatted: String = "00:00",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val allActivities: List<ActivityItem> = emptyList() // Для маппинга ID в имя
)

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
    private val timerSessionDao: TimerSessionDao, // Прямой доступ к DAO сессий
    private val authService: AuthService
    // private val timerService: TimerService // Альтернатива для получения сессий, если он предоставляет нужные Flow
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsScreenUiState(isLoading = true))
    val uiState: StateFlow<StatsScreenUiState> = _uiState.asStateFlow()

    private val selectedDateFlow = MutableStateFlow(getStartOfDay(Date())) // Храним начало дня

    init {
        viewModelScope.launch {
            val userId = authService.getCurrentUserId()
            if (userId == null) {
                _uiState.value = StatsScreenUiState(isLoading = false, errorMessage = "Пользователь не найден")
                return@launch
            }
            // Загружаем все активности один раз для маппинга ID в имя
            try {
                val activities = activityRepository.getAllActivities(userId)
                _uiState.update { it.copy(allActivities = activities) }
            } catch (e: Exception) {
                Log.e("StatsVM", "Error loading all activities", e)
                // Обработка ошибки загрузки активностей
            }


            // Подписываемся на изменения выбранной даты
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
                    _uiState.update { it.copy(isLoading = false) }
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
            SessionDisplayItem(
                activityName = activityName,
                startTimeFormatted = formatTimeForSessionList(session.startTime),
                endTimeFormatted = formatTimeForSessionList(session.endTime),
                durationFormatted = formatDurationViewModel(
                    (session.endTime?.time ?: session.startTime.time) - session.startTime.time // Если endTime null, сессия еще идет? (не должно быть в статистике)
                ),
                originalSession = session
            )
        }

        val activitySummary = sessions
            .groupBy { it.activityId }
            .mapNotNull { (activityId, sessionsForActivity) ->
                val activity = currentAllActivities.find { it.id == activityId }
                if (activity != null) {
                    val totalMillis = sessionsForActivity.sumOf {
                        (it.endTime?.time ?: it.startTime.time) - it.startTime.time // Учитывать только завершенные для статистики
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
                totalTimeForSelectedDateFormatted = formatDurationViewModel(totalTimeForDayMillis, forceHours = true)
            )
        }
    }

    // Вспомогательные функции для получения начала и конца дня
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


