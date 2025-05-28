package ru.hoster.inprogress.navigation

import ru.hoster.inprogress.data.ActivityItem // Assuming ActivityItem is accessible
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date

/**
 * Represents the statistics for a single day in the calendar view.
 */
data class CalendarDayStat(
    val date: LocalDate,
    val totalDurationMillis: Long,
    val totalDurationFormatted: String, // e.g., "1h 30m"
    val isToday: Boolean,
    val isInSelectedMonth: Boolean
)

/**
 * Represents a slice in the activity distribution pie chart.
 */
data class ActivityPieSlice(
    val activityId: Long,
    val activityName: String,
    val durationMillis: Long,
    val durationFormatted: String, // e.g., "10h 15m"
    val percentage: Float, // 0.0 to 1.0
    val colorHex: String // From ActivityItem.colorHex
)

/**
 * Main UI state for the Monthly Statistics screen.
 */
data class MonthlyStatsUiState(
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val monthDisplayName: String = "", // e.g., "Май 2025"
    val calendarDays: List<CalendarDayStat> = emptyList(), // Includes days from previous/next month for full weeks
    val monthTotalDurationMillis: Long = 0L,
    val monthTotalDurationFormatted: String = "0ч 0м",
    val previousMonthComparisonValue: Long = 0L, // Raw difference in millis
    val previousMonthComparisonFormatted: String = "Пока нет данных за прошлый месяц", // e.g., "+10ч 5м (20%) по сравнению с Апрелем"
    val activityDistribution: List<ActivityPieSlice> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
