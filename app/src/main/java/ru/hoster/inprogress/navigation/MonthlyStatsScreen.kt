package ru.hoster.inprogress.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.hoster.inprogress.navigation.parseColor // Import the helper
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyStatsScreen(
    viewModel: MonthlyStatsViewModel = hiltViewModel()
    // navController: NavController // For navigating back if needed
) {
    val uiState by viewModel.uiState.collectAsState()
    val russianLocale = Locale("ru")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.monthDisplayName.ifEmpty { "Статистика за месяц" }) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onPreviousMonthClicked() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущий месяц")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onNextMonthClicked() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующий месяц")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(
                    text = "Ошибка: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn( // Changed to LazyColumn to allow scrolling of all content
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item { CalendarHeader(russianLocale) }
                item { Spacer(Modifier.height(4.dp)) }

                // Calendar Grid
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp), // Adjust height as needed or make it dynamic
                        userScrollEnabled = false // The parent LazyColumn handles scrolling
                    ) {
                        items(uiState.calendarDays, key = { it.date.toString() }) { dayStat ->
                            CalendarDayCell(dayStat)
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }

                // Monthly Summary
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

                // Activity Distribution (Pie Chart and Legend)
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
                    // Placeholder for Pie Chart - You can integrate a library or custom Canvas drawing here
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp) // Placeholder height
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Место для круговой диаграммы")
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }

                    // Legend for Activity Distribution
                    uiState.activityDistribution.forEach { slice ->
                        item { ActivityLegendItem(slice) }
                        item { Divider(modifier = Modifier.padding(vertical = 4.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarHeader(locale: Locale) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Get days of the week starting from Monday for Russian locale
        val daysOfWeek = DayOfWeek.entries.toMutableList()
        // Adjust to start from Monday if locale indicates it (DayOfWeek.MONDAY is 1, SUNDAY is 7)
        val firstDayOfWeek = java.time.temporal.WeekFields.of(locale).firstDayOfWeek.value // MONDAY is 1
        while(daysOfWeek.first().value != firstDayOfWeek) {
            val day = daysOfWeek.removeAt(0)
            daysOfWeek.add(day)
        }

        daysOfWeek.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CalendarDayCell(dayStat: CalendarDayStat) {
    val backgroundColor = when {
        dayStat.isToday -> MaterialTheme.colorScheme.primaryContainer
        !dayStat.isInSelectedMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        dayStat.totalDurationMillis > 0 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        dayStat.isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        !dayStat.isInSelectedMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        dayStat.totalDurationMillis > 0 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (dayStat.isToday) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Makes cells square
            .padding(2.dp)
            .border(1.dp, borderColor)
            .background(backgroundColor)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayStat.date.dayOfMonth.toString(),
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (dayStat.isInSelectedMonth) FontWeight.Medium else FontWeight.Light
            )
            if (dayStat.isInSelectedMonth && dayStat.totalDurationFormatted.isNotBlank()) {
                Text(
                    text = dayStat.totalDurationFormatted,
                    color = textColor.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ActivityLegendItem(slice: ActivityPieSlice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(parseColor(slice.colorHex))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = slice.activityName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "${slice.durationFormatted} (${"%.1f".format(slice.percentage * 100)}%)",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
