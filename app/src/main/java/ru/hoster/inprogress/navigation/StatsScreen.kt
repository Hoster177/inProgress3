package ru.hoster.inprogress.navigation // Or your actual package

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // Keep for Daily view
import androidx.compose.foundation.lazy.grid.GridCells // For Monthly Calendar
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid // For Monthly Calendar
import androidx.compose.foundation.lazy.grid.items // For Monthly Calendar (different import)
import androidx.compose.foundation.lazy.items // Keep for Daily view
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.Canvas // For drawing the pie chart
import androidx.compose.ui.graphics.drawscope.Stroke // For potential donut chart style
import androidx.compose.ui.graphics.drawscope.Fill // To fill the slices
import androidx.compose.ui.geometry.Size // For defining arc bounds

// Enum for tab selection (if not defined elsewhere)
enum class StatsViewType(val title: String) {
    Daily("По дням"),
    Monthly("По месяцам")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    dailyViewModel: StatsViewModel = hiltViewModel(), // This ViewModel should expose DailyStatsUiState
    // navController: NavController // For navigating back, etc.
) {
    var selectedTab by remember { mutableStateOf(StatsViewType.Daily) }
    val monthlyViewModel: MonthlyStatsViewModel = hiltViewModel()

    val dailyUiState by dailyViewModel.uiState.collectAsState() // This should now be DailyStatsUiState
    val monthlyUiState by monthlyViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                actions = {
                    if (selectedTab == StatsViewType.Monthly) {
                        IconButton(onClick = { monthlyViewModel.onPreviousMonthClicked() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущий месяц")
                        }
                        IconButton(onClick = { monthlyViewModel.onNextMonthClicked() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующий месяц")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                StatsViewType.entries.forEach { tabType ->
                    Tab(
                        selected = selectedTab == tabType,
                        onClick = { selectedTab = tabType },
                        text = { Text(tabType.title) }
                    )
                }
            }

            when (selectedTab) {
                StatsViewType.Daily -> {
                    DailyStatsContent(
                        uiState = dailyUiState, // This now expects DailyStatsUiState and should receive it
                        onDateSelected = { dailyViewModel.selectDate(it) },
                        modifier = Modifier.padding(16.dp)
                    )
                }
                StatsViewType.Monthly -> {
                    MonthlyStatsContent(
                        uiState = monthlyUiState,
                        modifier = Modifier.padding(0.dp) // Monthly content will have its own LazyColumn padding
                    )
                }
            }
        }
    }
}

@Composable
fun DailyStatsContent(
    uiState: DailyStatsUiState, // Expects DailyStatsUiState
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateDialogState = rememberMaterialDialogState()

    Column(modifier = modifier.fillMaxSize()) {
        // Date Picker Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { dateDialogState.show() }.padding(vertical = 8.dp)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = "Выбрать дату")
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Дата: ${uiState.selectedDateFormatted}",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (uiState.errorMessage != null) {
            Text(
                "Ошибка: ${uiState.errorMessage}",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text(
                "Всего за день: ${uiState.totalTimeForSelectedDateFormatted}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Text("По активностям:", style = MaterialTheme.typography.titleMedium)
            if (uiState.activitySummaryForSelectedDate.isEmpty()) {
                Text("Нет данных по активностям за выбранный день.", style = MaterialTheme.typography.bodyMedium)
            } else {
                uiState.activitySummaryForSelectedDate.forEach { activityStats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(activityStats.activityName, modifier = Modifier.weight(1f))
                        Text(activityStats.totalDurationFormatted)
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(24.dp))

            Text("Сессии за день:", style = MaterialTheme.typography.titleMedium)
            if (uiState.sessionsForSelectedDate.isEmpty()) {
                Text("Нет сессий за выбранный день.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) { // This LazyColumn is for daily sessions
                    items(uiState.sessionsForSelectedDate) { sessionDisplay ->
                        SessionRow(sessionDisplay)
                        Divider()
                    }
                }
            }
        }
    }

    MaterialDialog(
        dialogState = dateDialogState,
        buttons = {
            positiveButton("ОК")
            negativeButton("Отмена")
        }
    ) {
        datepicker(
            initialDate = uiState.selectedDate.toLocalDate(),
            title = "Выберите дату",
            allowedDateValidator = { it.isBefore(LocalDate.now().plusDays(1)) }
        ) { date ->
            onDateSelected(date.toDate())
        }
    }
}


@Composable
fun SessionRow(session: SessionDisplayItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(session.activityName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${session.startTimeFormatted} - ${session.endTimeFormatted}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(session.durationFormatted, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
    }
}

fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

fun LocalDate.toDate(): Date {
    return Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
}


data class ActivityStats(val activityName: String, val totalDurationFormatted: String)

// Add this composable function within your StatsScreen.kt or a relevant UI utility file

@Composable
fun PieChart(
    slices: List<ActivityPieSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 0f // Set to > 0f for a donut chart, 0f for a filled pie chart
) {
    if (slices.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Нет данных для диаграммы")
        }
        return
    }

    val totalPercentage = slices.sumOf { it.percentage.toDouble() }.toFloat() // Should be close to 1.0f

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val diameter = minOf(canvasWidth, canvasHeight) * 0.9f // Use 90% of the smaller dimension
        val radius = diameter / 2f
        val topLeftX = (canvasWidth - diameter) / 2f
        val topLeftY = (canvasHeight - diameter) / 2f

        var startAngle = -90f // Start at 12 o'clock

        slices.forEach { slice ->
            val sweepAngle = (slice.percentage / totalPercentage) * 360f // Normalize sweep angle
            val sliceColor = parseColor(slice.colorHex)

            drawArc(
                color = sliceColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true, // True for pie, false for arc outline
                topLeft = androidx.compose.ui.geometry.Offset(topLeftX, topLeftY),
                size = Size(diameter, diameter),
                style = if (strokeWidth > 0f) Stroke(width = strokeWidth) else Fill
            )
            startAngle += sweepAngle
        }
    }
}

// In StatsScreen.kt

// ... (other composables like DailyStatsContent, CalendarHeader, etc.)

@Composable
fun MonthlyStatsContent(
    uiState: MonthlyStatsUiState, // From MonthlyStatsViewModel
    modifier: Modifier = Modifier
) {
    val russianLocale = Locale("ru")

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Ошибка: ${uiState.error}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        LazyColumn( // This LazyColumn is for the entire monthly stats content
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text( // Month display now part of content, as TopAppBar is shared
                    text = uiState.monthDisplayName,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
            item { CalendarHeader(russianLocale) }
            item { Spacer(Modifier.height(4.dp)) }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    userScrollEnabled = false
                ) {
                    items(uiState.calendarDays, key = { it.date.toString() }) { dayStat ->
                        CalendarDayCell(dayStat)
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            item {
                Text(
                    "Всего за ${uiState.selectedYearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, russianLocale)}: ${uiState.monthTotalDurationFormatted}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text(
                    uiState.previousMonthComparisonFormatted,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            item { Spacer(Modifier.height(24.dp)) }

            item {
                Text(
                    "Распределение по задачам:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item { Spacer(Modifier.height(8.dp)) }

            if (uiState.activityDistribution.isEmpty()) {
                item { Text("Нет данных по задачам за этот месяц.") }
            } else {
                item {
                    // Integrate the actual PieChart composable here
                    PieChart(
                        slices = uiState.activityDistribution,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp) // Adjust height as desired
                            .padding(vertical = 16.dp)
                    )
                }
                // Legend for Activity Distribution (already present)
                uiState.activityDistribution.forEach { slice ->
                    item { ActivityLegendItem(slice) }
                    item { Divider(modifier = Modifier.padding(vertical = 4.dp)) }
                }
            }
        }
    }
}

