package ru.hoster.inprogress.navigation
import android.app.Application
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.internal.Contexts.getApplication
import ru.hoster.inprogress.data.ActivityItem
import ru.hoster.inprogress.data.Goal
import ru.hoster.inprogress.data.GoalType
import ru.hoster.inprogress.service.TimerService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.TimeZone // For UTC as requested by user in meta


data class MainScreenUiStatePlaceholder(
    val isLoading: Boolean = false,
    val userName: String = "User",
    val activities: List<String> = emptyList(),
    val currentDate: String = getCurrentDateString(),
    val dailyTotalTimeFormatted: String = "00:00:00",
    val goals: List<Goal> = emptyList()
)
fun formatDuration(millis: Long, forceHours: Boolean = false): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0 || forceHours) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainScreenUiState,
    onDailyTimerClick: () -> Unit,
    onAddActivityClick: () -> Unit,
    onEditGoalClick: (goalId: String) -> Unit,
    onDeleteActivityClick: (activityId: String) -> Unit,
    onActivityTimerToggle: (activityId: String,/* currentActiveState: Boolean*/) -> Unit,
    onAddNewGoalClick: () -> Unit,
    onViewAllGoalsClick: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddActivityClick) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить занятие")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else {
                Text(
                    text = uiState.currentDate,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onDailyTimerClick() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Всего за день", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = uiState.dailyTotalTimeFormatted,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
//                Spacer(modifier = Modifier.height(16.dp))
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        "Цели (${uiState.goals.take(3).size}/${uiState.goals.size})",
//                        style = MaterialTheme.typography.titleLarge
//                    )
//                    TextButton(onClick = onViewAllGoalsClick) {
//                        Text("Все цели")
//                    }
//                }
//                Spacer(modifier = Modifier.height(8.dp))
//                if (uiState.goals.isEmpty()) {
//                    Text(
//                        "Нет активных целей. Нажмите 'Все цели', чтобы добавить или изменить.",
//                        modifier = Modifier.padding(vertical = 8.dp),
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                } else {
//                    uiState.goals.take(3).forEach { goal ->
//                        val idToPass = goal.firebaseId ?: goal.id.toString()
//                        GoalItem(
//                            goal = goal,
//                            onEditClick = { onEditGoalClick(idToPass) }
//                        )
//                    }
//                }
//                TextButton(
//                    onClick = onAddNewGoalClick,
//                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
//                ) {
//                    Text("Добавить новую цель")
//                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Занятия",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.activities.isEmpty()) {
                    Text(
                        "Нет занятий на сегодня. Нажмите '+' чтобы добавить.",
                        modifier = Modifier.padding(vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(
                            items = uiState.activities,
                            key = { activityUi ->
                                activityUi.baseActivity.firebaseId ?: activityUi.baseActivity.id.toString()
                            }
                        ) { activityUi -> // Имя параметра изменено на activityUi
                            val idToPass = activityUi.baseActivity.firebaseId ?: activityUi.baseActivity.id.toString()
                            ActivityItemRow(
                                activityUi = activityUi,
                                onTimerToggle = { onActivityTimerToggle(idToPass) },
                                onDeleteClick = { onDeleteActivityClick(idToPass) }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalItem(goal: Goal, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEditClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when (goal.type) {
                        GoalType.TIME_PER_PERIOD ->
                            "${formatDuration(goal.currentProgressMillis)} / ${formatDuration(goal.targetDurationMillis ?: 0L)} за ${goal.periodDays ?: 0} дней"
                        GoalType.CONSECUTIVE_DAYS ->
                            "${goal.currentConsecutiveDays} / ${goal.targetConsecutiveDays ?: 0} дней подряд"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Редактировать цель")
            }
        }
    }
}

@Composable
fun ActivityItemRow(
    activityUi: ActivityItemUi,
    onTimerToggle: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(activityUi.baseActivity.name, style = MaterialTheme.typography.titleMedium)
                Text(

                    "Сегодня: ${activityUi.displayDurationFormatted}",
                    style = MaterialTheme.typography.bodyMedium
                )

            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTimerToggle) {
                    Icon(

                        if (activityUi.isCurrentlyActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (activityUi.isCurrentlyActive) "Пауза" else "Старт",
                        tint = if (activityUi.isCurrentlyActive) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить занятие")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Подтвердить удаление") },
            text = { Text("Вы уверены, что хотите удалить занятие \"${activityUi.baseActivity.name}\"? Все данные будут удалены.") }, // <--- ИСПРАВЛЕНИЕ ЗДЕСЬ
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }
}

fun getCurrentDateString(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
    return sdf.format(Date())
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun MainScreenPreview_Empty() {
    MaterialTheme {
        MainScreen(
            uiState = MainScreenUiState(
                currentDate = "21 мая 2025",
                dailyTotalTimeFormatted = "00:00:00",
                goals = emptyList(),
                activities = emptyList()
            ),
            onDailyTimerClick = {},
            onAddActivityClick = {},
            onEditGoalClick = {},
            onDeleteActivityClick = {},
            onActivityTimerToggle = { _-> },
            onAddNewGoalClick = {},
            onViewAllGoalsClick = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun MainScreenPreview_WithData() {
    val sampleGoals = listOf(
        Goal(id = 1L, firebaseId = "g1", userId = "uid1", title = "Проект Альфа", description = "Завершить основной функционал", type = GoalType.TIME_PER_PERIOD, targetDurationMillis = 10 * 3600000L, currentProgressMillis = 2 * 3600000L, periodDays = 7, createdAt = Date()),
        Goal(id = 2L, firebaseId = "g2", userId = "uid1", title = "Ежедневное чтение", description = "Читать по 30 минут", type = GoalType.CONSECUTIVE_DAYS, targetConsecutiveDays = 5, currentConsecutiveDays = 2, createdAt = Date())
    )
    val sampleActivities = listOf(
        ActivityItem(id = 1, firebaseId = "a1", userId = "uid1", name = "Разработка UI", totalDurationMillisToday = 3600000L, isActive = true, createdAt = Date()),
        ActivityItem(id = 2, firebaseId = "a2", userId = "uid1", name = "Встреча с командой", totalDurationMillisToday = 1800000L, isActive = false, createdAt = Date())
    )

    val sampleActivitiesUi = sampleActivities.map {
        ActivityItemUi(
            baseActivity = it,
            displayDurationMillis = it.totalDurationMillisToday,
            displayDurationFormatted = formatDuration(it.totalDurationMillisToday),
            isCurrentlyActive = it.isActive
        )
    }
    MaterialTheme {
        MainScreen(
            uiState = MainScreenUiState(
                currentDate = "21 мая 2025",
                dailyTotalTimeFormatted = "01:30:45",
                goals = sampleGoals,
                activities = sampleActivitiesUi
            ),
            onDailyTimerClick = {},
            onAddActivityClick = {},
            onEditGoalClick = {},
            onDeleteActivityClick = {},
            onActivityTimerToggle = { _-> },
            onAddNewGoalClick = {},
            onViewAllGoalsClick = {}
        )
    }
}


@Preview(showBackground = true)
@Composable
fun ActivityItemRowPreview_Active() {
    MaterialTheme {
        ActivityItemRow(
            activityUi = ActivityItemUi(
                baseActivity = ActivityItem(
                    id = 1,
                    userId = "previewUser",
                    name = "Кодинг фичи",
                    totalDurationMillisToday = 7425000L,
                    isActive = true,
                    createdAt = Date()
                ),
                displayDurationMillis = 7425000L,
                displayDurationFormatted = formatDuration(7425000L),
                isCurrentlyActive = true
            ),
            onTimerToggle = {},
            onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GoalItemPreview_Time() {
    MaterialTheme {
        GoalItem(
            goal = Goal(
                id = 1L,
                userId = "previewUser", // Required
                title = "Изучение Jetpack Compose", // Required
                description = "Посмотреть туториалы и сделать тестовый проект.", // Nullable, providing for completeness
                type = GoalType.TIME_PER_PERIOD, // Required
                currentProgressMillis = 1800000L,
                targetDurationMillis = 5 * 3600000L,
                periodDays = 7,
                createdAt = Date(), // Nullable, providing
                firebaseId = "g_preview_time", // Nullable
                targetConsecutiveDays = null, // Nullable
                currentConsecutiveDays = 0, // Defaulted
                deadline = null, // Nullable
                isAchieved = false, // Defaulted
                isDefault = false // Defaulted
            ),
            onEditClick = {}
        )
    }
}