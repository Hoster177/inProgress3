package ru.hoster.inprogress.navigation

import ru.hoster.inprogress.data.ActivityItem // Assuming ActivityItem is accessible
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date


data class CalendarDayStat(
    val date: LocalDate,
    val totalDurationMillis: Long,
    val totalDurationFormatted: String,
    val isToday: Boolean,
    val isInSelectedMonth: Boolean
)


data class ActivityPieSlice(
    val activityId: Long,
    val activityName: String,
    val durationMillis: Long,
    val durationFormatted: String,
    val percentage: Float,
    val colorHex: String
)


data class MonthlyStatsUiState(
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val monthDisplayName: String = "",
    val calendarDays: List<CalendarDayStat> = emptyList(),
    val monthTotalDurationMillis: Long = 0L,
    val monthTotalDurationFormatted: String = "0ч 0м",
    val previousMonthComparisonValue: Long = 0L,
    val previousMonthComparisonFormatted: String = "Пока нет данных за прошлый месяц",
    val activityDistribution: List<ActivityPieSlice> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
