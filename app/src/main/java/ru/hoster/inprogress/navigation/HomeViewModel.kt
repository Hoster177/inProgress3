package ru.hoster.inprogress.navigation // Убедись, что пакет правильный

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.hoster.inprogress.data.ActivityItem // Убедись, что импорт правильный
import ru.hoster.inprogress.data.Goal // Убедись, что импорт правильный
import ru.hoster.inprogress.domain.model.ActivityRepository
import ru.hoster.inprogress.domain.model.GoalRepository
// Замени на реальные пути к твоим репозиториям
// import ru.hoster.inprogress.domain.repository.ActivityRepository
// import ru.hoster.inprogress.domain.repository.GoalRepository
import ru.hoster.inprogress.service.TimerService // Убедись, что импорт правильный
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject



// Конец Placeholder

fun getCurrentDateStringViewModel(): String { // Переименовал, чтобы не конфликтовать, если оставишь в MainScreen.kt
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
    return sdf.format(Date())
}

fun formatDurationViewModel(millis: Long, forceHours: Boolean = false): String { // Переименовал
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0 || forceHours) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

data class MainScreenUiState(
    val currentDate: String = getCurrentDateString(),
    val dailyTotalTimeFormatted: String = "00:00:00",
    val goals: List<Goal> = emptyList(),
    val activities: List<ActivityItem> = emptyList(),
    val isLoading: Boolean = false
)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application, // Для запуска сервиса
    private val activityRepository: ActivityRepository, // Замени на свой реальный репозиторий
    private val goalRepository: GoalRepository // Замени на свой реальный репозиторий
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState(isLoading = true))
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Комбинируем потоки от активностей и целей
            combine(
                activityRepository.getActivitiesForTodayFlow(),
                goalRepository.getActiveGoalsFlow()
            ) { activities, goals ->
                // Рассчитываем общее время за день
                val dailyTotalMillis = activities.sumOf { it.totalDurationMillisToday }
                MainScreenUiState(
                    currentDate = getCurrentDateStringViewModel(),
                    dailyTotalTimeFormatted = formatDurationViewModel(dailyTotalMillis, forceHours = true),
                    goals = goals,
                    activities = activities,
                    isLoading = false
                )
            }.catch { throwable ->
                // Обработка ошибок загрузки
                _uiState.value = MainScreenUiState(isLoading = false) // Показать пустой экран или ошибку
                // Log.e("HomeViewModel", "Error loading data", throwable)
            }.collect { combinedState ->
                _uiState.value = combinedState
            }
        }
    }

    fun onActivityTimerToggle(activityId: String, currentActiveState: Boolean) {
        viewModelScope.launch {
            val activity = activityRepository.getActivityById(activityId) // Или найти в текущем _uiState.value.activities
            activity?.let {
                val intent = Intent(application, TimerService::class.java).apply {
                    putExtra(TimerService.EXTRA_TASK_ID, activityId)
                    putExtra(TimerService.EXTRA_TASK_NAME, it.name)
                    // Передаем текущее накопленное время, если сервис будет его использовать для инициализации
                    putExtra(TimerService.EXTRA_ACCUMULATED_TIME, it.totalDurationMillisToday)
                }

                if (currentActiveState) { // Если был активен, значит останавливаем
                    intent.action = TimerService.ACTION_STOP_TIMER
                    // Опционально: обновить локальное состояние isActive немедленно для UI
                    // activityRepository.updateActivity(it.copy(isActive = false))
                } else { // Если был неактивен, значит запускаем
                    // Перед запуском нового таймера, убедимся, что другие остановлены (если нужна такая логика)
                    // Это можно сделать, пройдясь по списку активных задач и послав ACTION_STOP_TIMER
                    // или сервис сам может обрабатывать только один активный таймер.
                    // Для простоты, предположим, сервис сам управляет уникальностью активного таймера.
                    intent.action = TimerService.ACTION_START_TIMER
                    // Опционально: обновить локальное состояние isActive немедленно для UI
                    // activityRepository.updateActivity(it.copy(isActive = true))
                }
                application.startService(intent)
            }
        }
        // Важно: `isActive` и `totalDurationMillisToday` в UI должны в итоге обновляться
        // через Flow из репозитория, после того как TimerService обновит БД.
        // Немедленное обновление `isActive` выше - это для отзывчивости UI, но БД - источник правды.
    }

    fun onDeleteActivityClick(activityId: String) {
        viewModelScope.launch {
            // Если удаляемая задача активна, нужно остановить её таймер
            val activity = _uiState.value.activities.find { (it.firebaseId ?: it.id.toString()) == activityId }
            if (activity?.isActive == true) {
                val intent = Intent(application, TimerService::class.java).apply {
                    action = TimerService.ACTION_STOP_TIMER
                    putExtra(TimerService.EXTRA_TASK_ID, activityId)
                }
                application.startService(intent)
            }
            activityRepository.deleteActivity(activityId)
            // UI обновится автоматически через Flow
        }
    }

    // Функции-заглушки для навигации, если они должны обрабатываться здесь
    // Либо они передаются напрямую в Composable из NavHost
    fun onDailyTimerClick() { /* TODO: Navigation to Statistics */ }
    fun onAddActivityClick() { /* TODO: Navigation to AddEditActivity */ }
    fun onEditGoalClick(goalId: String) { /* TODO: Navigation to AddEditGoal with goalId */ }
    fun onAddNewGoalClick() { /* TODO: Navigation to AddEditGoal */ }
    fun onViewAllGoalsClick() { /* TODO: Navigation to AllGoalsScreen */ }

}