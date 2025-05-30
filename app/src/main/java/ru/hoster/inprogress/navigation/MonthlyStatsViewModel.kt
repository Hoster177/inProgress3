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
import ru.hoster.inprogress.service.TimerService
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Calendar // For Date conversion
import java.util.Date // For DAO
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MonthlyStatsViewModel @Inject constructor(
    private val timerService: TimerService,
    private val activityRepository: ActivityRepository,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyStatsUiState())
    val uiState: StateFlow<MonthlyStatsUiState> = _uiState.asStateFlow()

    private val russianLocale = Locale("ru")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", russianLocale)

    init {
        loadStatsForMonth(YearMonth.now())
    }

    fun loadStatsForMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedYearMonth = yearMonth, error = null) }

            val userId = authService.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Пользователь не авторизован.") }
                return@launch
            }

            val allActivities = try {
                activityRepository.getEnrichedActivitiesFlow().first()
            } catch (e: Exception) {
                Log.e("MonthlyStatsVM", "Error fetching activities", e)
                _uiState.update { it.copy(isLoading = false, error = "Не удалось загрузить задачи.") }
                return@launch
            }
            val activitiesMap = allActivities.associateBy { it.id }


            val currentMonthStart = yearMonth.atDay(1).toDate()
            val currentMonthEnd = yearMonth.atEndOfMonth().toDateWithTime(23, 59, 59)

            val previousYearMonth = yearMonth.minusMonths(1)
            val previousMonthStart = previousYearMonth.atDay(1).toDate()
            val previousMonthEnd = previousYearMonth.atEndOfMonth().toDateWithTime(23, 59, 59)

            try {

                val currentMonthSessionsFlow = timerService.getSessionsForUserInDateRange(userId, currentMonthStart, currentMonthEnd)
                val previousMonthSessionsFlow = timerService.getSessionsForUserInDateRange(userId, previousMonthStart, previousMonthEnd)

                currentMonthSessionsFlow.combine(previousMonthSessionsFlow) { currentSessions, previousSessions ->
                    processSessions(
                        yearMonth,
                        currentSessions,
                        previousSessions,
                        activitiesMap
                    )
                }.catch { e ->
                    Log.e("MonthlyStatsVM", "Error processing sessions", e)
                    _uiState.update { it.copy(isLoading = false, error = "Ошибка при обработке данных сессий: ${e.message}") }
                }
                    .collect { processedState ->
                        _uiState.value = processedState
                    }

            } catch (e: Exception) {
                Log.e("MonthlyStatsVM", "Error loading stats", e)
                _uiState.update { it.copy(isLoading = false, error = "Не удалось загрузить статистику: ${e.message}") }
            }
        }
    }

    private fun processSessions(
        yearMonth: YearMonth,
        currentMonthSessions: List<TimerSession>,
        previousMonthSessions: List<TimerSession>,
        activitiesMap: Map<Long, ActivityItem>
    ): MonthlyStatsUiState {
        val dailyDurationsCurrentMonth = mutableMapOf<LocalDate, Long>()
        val activityDurationsCurrentMonth = mutableMapOf<Long, Long>()
        var currentMonthTotalMillis = 0L

        currentMonthSessions.forEach { session ->
            if (session.endTime != null) {
                val duration = session.endTime.time - session.startTime.time
                if (duration > 0) {
                    currentMonthTotalMillis += duration
                    val sessionDate = session.startTime.toLocalDate()
                    dailyDurationsCurrentMonth[sessionDate] = (dailyDurationsCurrentMonth[sessionDate] ?: 0L) + duration
                    activityDurationsCurrentMonth[session.activityId] = (activityDurationsCurrentMonth[session.activityId] ?: 0L) + duration
                }
            }
        }


        var previousMonthTotalMillis = 0L
        previousMonthSessions.forEach { session ->
            if (session.endTime != null) {
                val duration = session.endTime.time - session.startTime.time
                if (duration > 0) {
                    previousMonthTotalMillis += duration
                }
            }
        }


        val calendarDays = generateCalendarDays(yearMonth, dailyDurationsCurrentMonth)


        val activityDistribution = activityDurationsCurrentMonth.mapNotNull { (activityId, duration) ->
            activitiesMap[activityId]?.let { activity ->
                ActivityPieSlice(
                    activityId = activityId,
                    activityName = activity.name,
                    durationMillis = duration,
                    durationFormatted = formatDuration(duration),
                    percentage = if (currentMonthTotalMillis > 0) duration.toFloat() / currentMonthTotalMillis else 0f,
                    colorHex = activity.colorHex!!.ifEmpty { "#CCCCCC" }
                )
            }
        }.sortedByDescending { it.durationMillis }


        val comparisonValue = currentMonthTotalMillis - previousMonthTotalMillis
        val comparisonFormatted = formatMonthComparison(currentMonthTotalMillis, previousMonthTotalMillis, yearMonth.minusMonths(1))

        return MonthlyStatsUiState(
            selectedYearMonth = yearMonth,
            monthDisplayName = yearMonth.format(monthYearFormatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() },
            calendarDays = calendarDays,
            monthTotalDurationMillis = currentMonthTotalMillis,
            monthTotalDurationFormatted = formatDuration(currentMonthTotalMillis, forceHours = true),
            previousMonthComparisonValue = comparisonValue,
            previousMonthComparisonFormatted = comparisonFormatted,
            activityDistribution = activityDistribution,
            isLoading = false,
            error = null
        )
    }


    private fun generateCalendarDays(yearMonth: YearMonth, dailyDurations: Map<LocalDate, Long>): List<CalendarDayStat> {
        val days = mutableListOf<CalendarDayStat>()
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()

        val firstDayOfCalendar = firstDayOfMonth.with(WeekFields.of(russianLocale).firstDayOfWeek).with(TemporalAdjusters.previousOrSame(WeekFields.of(russianLocale).firstDayOfWeek))


        var currentDay = firstDayOfCalendar
        val today = LocalDate.now()

        for (i in 0 until 42) { // 6 weeks * 7 days
            val duration = dailyDurations[currentDay] ?: 0L
            days.add(
                CalendarDayStat(
                    date = currentDay,
                    totalDurationMillis = duration,
                    totalDurationFormatted = if (duration > 0) formatDuration(duration) else "",
                    isToday = currentDay.isEqual(today),
                    isInSelectedMonth = currentDay.month == yearMonth.month
                )
            )
            currentDay = currentDay.plusDays(1)

            if (currentDay.monthValue != yearMonth.monthValue && currentDay.dayOfWeek == WeekFields.of(russianLocale).firstDayOfWeek && days.size >=35) {
                if (days.any{it.date.dayOfMonth == lastDayOfMonth.dayOfMonth && it.date.monthValue == lastDayOfMonth.monthValue} || currentDay.isAfter(lastDayOfMonth)) break
            }
        }
        return days
    }

    private fun formatMonthComparison(currentMonthMillis: Long, previousMonthMillis: Long, previousYearMonth: YearMonth): String {
        if (previousMonthMillis == 0L) {
            return if (currentMonthMillis > 0) "Данных за ${previousYearMonth.format(monthYearFormatter)} нет."
            else "Нет данных за этот и прошлый месяц."
        }
        val diff = currentMonthMillis - previousMonthMillis
        val percentageChange = (diff.toDouble() / previousMonthMillis * 100)

        val diffSign = if (diff >= 0) "+" else ""
        val formattedDiff = formatDuration(abs(diff), short = true)
        val prevMonthName = previousYearMonth.format(DateTimeFormatter.ofPattern("LLLL", russianLocale))

        return String.format(russianLocale, "%s%s (%.0f%%) по сравнению с %s",
            diffSign, formattedDiff, percentageChange, prevMonthName)
    }

    fun formatDuration(millis: Long, forceHours: Boolean = false, short: Boolean = false): String {
        if (millis <= 0) return if (forceHours || short) "0ч 0м" else "0м 0с"

        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return when {
            short -> {
                if (hours > 0) String.format(russianLocale, "%dч %dм", hours, minutes)
                else String.format(russianLocale, "%dм", minutes)
            }
            forceHours || hours > 0 -> String.format(russianLocale, "%dч %02dм %02dс", hours, minutes, seconds)
            else -> String.format(russianLocale, "%dм %02dс", minutes, seconds)
        }
    }


    fun onNextMonthClicked() {
        val current = _uiState.value.selectedYearMonth
        loadStatsForMonth(current.plusMonths(1))
    }

    fun onPreviousMonthClicked() {
        val current = _uiState.value.selectedYearMonth
        loadStatsForMonth(current.minusMonths(1))
    }


    private fun LocalDate.toDate(): Date {
        return Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }
    private fun LocalDate.toDateWithTime(hour: Int, minute: Int, second: Int): Date {
        return Date.from(this.atTime(hour,minute,second).atZone(ZoneId.systemDefault()).toInstant())
    }

    private fun Date.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this.time).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    private fun abs(value: Long): Long = if (value < 0) -value else value
}

