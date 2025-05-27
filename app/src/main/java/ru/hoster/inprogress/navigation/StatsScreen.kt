package ru.hoster.inprogress.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
// Импорты для Material Design 3:
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
// import androidx.compose.material3.rememberTopAppBarState // Если нужна сложная логика с TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Общий импорт для Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vanpra.composematerialdialogs.MaterialDialog // Эта библиотека должна быть совместима
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import ru.hoster.inprogress.navigation.SessionDisplayItem
import ru.hoster.inprogress.navigation.StatsViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class) // Для TopAppBar и Scaffold в M3
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
    // navController: NavController // Для навигации назад и т.д.
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateDialogState = rememberMaterialDialogState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                // colors = TopAppBarDefaults.topAppBarColors( // Пример кастомизации цветов M3
                //    containerColor = MaterialTheme.colorScheme.primaryContainer,
                //    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                // )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Выбор даты
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { dateDialogState.show() }
            ) {
                Icon(Icons.Default.DateRange, contentDescription = "Выбрать дату")
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Дата: ${uiState.selectedDateFormatted}",
                    style = MaterialTheme.typography.titleLarge // Замена для h6 в M3
                )
            }
            Spacer(Modifier.height(16.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.errorMessage != null) {
                Text(
                    "Ошибка: ${uiState.errorMessage}",
                    color = MaterialTheme.colorScheme.error // Замена для colors.error в M3
                )
            } else {
                // Общее время за день
                Text(
                    "Всего за день: ${uiState.totalTimeForSelectedDateFormatted}",
                    style = MaterialTheme.typography.headlineSmall, // Замена для h5 в M3
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                // Суммарно по активностям
                Text("По активностям:", style = MaterialTheme.typography.titleMedium) // Замена для h6 или titleLarge
                if (uiState.activitySummaryForSelectedDate.isEmpty()){
                    Text("Нет данных по активностям за выбранный день.")
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

                // Список сессий
                Text("Сессии за день:", style = MaterialTheme.typography.titleMedium) // Замена
                if (uiState.sessionsForSelectedDate.isEmpty()){
                    Text("Нет сессий за выбранный день.")
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(uiState.sessionsForSelectedDate) { sessionDisplay ->
                            SessionRow(sessionDisplay) // SessionRow не меняется сильно
                            Divider()
                        }
                    }
                }
            }
        }
    }

    MaterialDialog( // Эта библиотека должна работать с M3, но проверьте ее документацию
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
            viewModel.selectDate(date.toDate())
        }
    }
}

@Composable
fun SessionRow(session: SessionDisplayItem) { // В основном без изменений, стили текста могут наследоваться
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
                fontSize = 12.sp, // Можно использовать MaterialTheme.typography.labelSmall
                color = MaterialTheme.colorScheme.onSurfaceVariant // Более подходящий цвет для второстепенного текста в M3
            )
        }
        Text(session.durationFormatted, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
    }
}

// Вспомогательные функции Date <-> LocalDate остаются теми же
fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

fun LocalDate.toDate(): Date {
    return Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
}