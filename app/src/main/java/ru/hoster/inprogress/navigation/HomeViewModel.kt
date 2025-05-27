package ru.hoster.inprogress.navigation // Убедись, что пакет правильный

import android.app.Application
// import android.content.Intent // No longer needed for TimerService direct calls
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.hoster.inprogress.data.ActivityItem // Убедись, что импорт правильный
import ru.hoster.inprogress.data.Goal // Убедись, что импорт правильный
import ru.hoster.inprogress.domain.model.ActivityRepository
import ru.hoster.inprogress.domain.model.AuthService
import ru.hoster.inprogress.domain.model.GoalRepository
import ru.hoster.inprogress.service.ActiveTimerInfo
import ru.hoster.inprogress.service.TimerService // Импортируем наш новый TimerService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// SessionUi and formatting functions remain the same
data class SessionUi(
    val sessionId: Long,
    val activityId: Long,
    val activityName: String,
    val startTime: Date,
    val endTime: Date?,
    val durationMillis: Long,
    val durationFormatted: String
)

fun getCurrentDateStringViewModel(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
    return sdf.format(Date())
}

fun formatDurationViewModel(millis: Long, forceHours: Boolean = false): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0 || forceHours) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}




// это поле будет обновляться ViewModel-ом, а не приходить из репозитория.
data class ActivityItemUi(
    val baseActivity: ActivityItem, // Оригинальный ActivityItem из репозитория
    val displayDurationMillis: Long, // Суммарная длительность для отображения (включая тикающую сессию)
    val displayDurationFormatted: String,
    val isCurrentlyActive: Boolean // Явно указываем, активна ли она СЕЙЧАС по данным таймера ViewModel
)

data class MainScreenUiState(
    val currentDate: String = getCurrentDateStringViewModel(),
    val dailyTotalTimeFormatted: String = "00:00:00", // Общее время за день из репозитория
    val goals: List<Goal> = emptyList(),
    val activities: List<ActivityItemUi> = emptyList(), // Используем ActivityItemUi
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentlyTimingActivityId: Long? = null, // ID задачи, для которой тикает таймер
    val currentlyTimingDurationFormatted: String? = null // Отформатированное время текущей сессии
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val goalRepository: GoalRepository,
    private val authService: AuthService,
    private val timerService: TimerService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState(isLoading = true))
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private fun getCurrentUserId(): String? = authService.getCurrentUserId()

    init {
        // Основные данные (список активностей, цели)
        loadInitialData()

        // Подписка на тикающий таймер из TimerService
        viewModelScope.launch {
            timerService.activeTimerFlow
                //.distinctUntilChanged() // Реагировать только на реальные изменения
                .collect { activeTimerInfo: ActiveTimerInfo? ->
                    _uiState.update { currentState ->
                        if (activeTimerInfo != null) {
                            Log.d("HomeVM_Ticker", "Received active timer info: ActID=${activeTimerInfo.activityId}, Duration=${activeTimerInfo.currentDurationMillis}")
                            // Обновляем ActivityItemUi и общее время
                            val updatedActivities = currentState.activities.map { activityUi ->
                                if (activityUi.baseActivity.id == activeTimerInfo.activityId) {
                                    // --- НАЧАЛО ВАЖНОЙ ЧАСТИ ---
                                    // Вариант 1: displayDuration - это ТОЛЬКО текущая сессия.
                                    // totalDurationMillisToday из baseActivity остается как есть (сумма всех завершенных + активная на момент загрузки из БД)
                                    // activityUi.copy(
                                    //    displayDurationMillis = activeTimerInfo.currentDurationMillis, // Показываем только тикающее время
                                    //    displayDurationFormatted = formatDurationViewModel(activeTimerInfo.currentDurationMillis),
                                    //    isCurrentlyActive = true
                                    // )

                                    // Вариант 2: displayDuration - это сумма ВСЕХ предыдущих сессий + текущая тикающая.
                                    // Требует, чтобы baseActivity.totalDurationMillisToday НЕ включало текущую активную сессию,
                                    // или чтобы у нас было поле вроде baseActivity.totalDurationOfCompletedSessions.
                                    // Если ActivityRepositoryImpl.getActivitiesForTodayFlow() уже правильно вычисляет
                                    // totalDurationMillisToday и isActive, то этот вариант самый правильный.
                                    // Предположим, getActivitiesForTodayFlow уже дал нам ActivityItem, где totalDurationMillisToday - это сумма
                                    // всех завершенных сессий, а isActive показывает, есть ли активная.
                                    // Тогда мы просто добавляем к этому длительность текущей сессии.
                                    // НО! Если getActivitiesForTodayFlow уже включает активную сессию (до System.currentTimeMillis на момент запроса),
                                    // то простое сложение будет неверным.

                                    // САМЫЙ НАДЕЖНЫЙ ВАРИАНТ ДЛЯ НАЧАЛА:
                                    // 1. `ActivityRepositoryImpl.getActivitiesForTodayFlow()` вычисляет `totalDurationMillisToday`
                                    //    как сумму ВСЕХ сессий (включая активную до `System.currentTimeMillis()` НА МОМЕНТ ЗАПРОСА К БД)
                                    //    и правильно выставляет `isActive`.
                                    // 2. В `HomeViewModel`, для АКТИВНОЙ задачи, мы ЗАМЕНЯЕМ ее `displayDurationMillis`
                                    //    на `activeTimerInfo.currentDurationMillis`.
                                    // 3. Общий `dailyTotalTimeFormatted` пересчитывается с учетом этой замены.
                                    activityUi.copy(
                                        displayDurationMillis = activeTimerInfo.currentDurationMillis, // Отображаем длительность ТЕКУЩЕЙ сессии
                                        displayDurationFormatted = formatDurationViewModel(activeTimerInfo.currentDurationMillis),
                                        isCurrentlyActive = true
                                    )
                                    // --- КОНЕЦ ВАЖНОЙ ЧАСТИ ---
                                } else {
                                    // Для неактивных задач, отображаем их полную сохраненную длительность
                                    activityUi.copy(
                                        displayDurationMillis = activityUi.baseActivity.totalDurationMillisToday,
                                        displayDurationFormatted = formatDurationViewModel(activityUi.baseActivity.totalDurationMillisToday),
                                        isCurrentlyActive = false
                                    )
                                }
                            }
                            // Пересчет общего времени за день
                            val newDailyTotal = updatedActivities.sumOf { it.displayDurationMillis }


                            currentState.copy(
                                activities = updatedActivities,
                                currentlyTimingActivityId = activeTimerInfo.activityId,
                                currentlyTimingDurationFormatted = formatDurationViewModel(activeTimerInfo.currentDurationMillis),
                                dailyTotalTimeFormatted = formatDurationViewModel(newDailyTotal, forceHours = true) // Обновляем общее время
                            )
                        } else {
                            // Нет активного таймера
                            Log.d("HomeVM_Ticker", "Received null active timer info (timer stopped).")
                            currentState.copy(
                                activities = currentState.activities.map { it.copy(isCurrentlyActive = false) }, // Все неактивны
                                currentlyTimingActivityId = null,
                                currentlyTimingDurationFormatted = null
                                // dailyTotalTimeFormatted остается от последнего значения из репозитория
                            )
                        }
                    }
                }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) { /* ... обработка ... */ return@launch }

            Log.d("HomeVM", "loadInitialData called for user: $userId")
            // Используем combine для получения базовых данных и данных о текущем таймере
            combine(
                activityRepository.getActivitiesForTodayFlow(),
                goalRepository.getActiveGoalsFlow()
                // timerService.activeTimerFlow // Можно и здесь, но тогда combine будет срабатывать каждую секунду.
                // Лучше отдельный collect для activeTimerFlow, как сделано выше.
            ) { activitiesFromRepo, goalsFromRepo ->
                Log.i("HomeVM_Combine", "Base data received. Activities: ${activitiesFromRepo.size}, Goals: ${goalsFromRepo.size}")

                // Преобразуем ActivityItem в ActivityItemUi
                // Здесь важно, как totalDurationMillisToday из репозитория соотносится с активной сессией.
                // Предположим, что getActivitiesForTodayFlow в репозитории уже вычисляет
                // totalDurationMillisToday и isActive на основе данных из TimerSessionDao.
                val activitiesUi = activitiesFromRepo.map { repoActivity ->
                    // Если таймер тикает для этой активности, ее isCurrentlyActive будет true,
                    // а displayDuration обновится в collect для activeTimerFlow.
                    // Начальное значение isCurrentlyActive берем из repoActivity.isActive.
                    ActivityItemUi(
                        baseActivity = repoActivity,
                        displayDurationMillis = repoActivity.totalDurationMillisToday,
                        displayDurationFormatted = formatDurationViewModel(repoActivity.totalDurationMillisToday),
                        isCurrentlyActive = repoActivity.isActive // Это из репозитория (основано на БД)
                    )
                }

                val dailyTotalMillis = activitiesUi.sumOf { it.displayDurationMillis } // или activitiesFromRepo.sumOf { it.totalDurationMillisToday }

                // _uiState НЕ обновляем здесь напрямую, если есть отдельный collect для activeTimerFlow,
                // чтобы не было конфликтов состояний. Этот combine только готовит "базовое" состояние.
                // Вместо этого, мы можем обновить _uiState, но нужно быть осторожным.
                // Лучше, чтобы этот combine обновлял только части UiState, не трогаемые activeTimerFlow.collect.
                // Или activeTimerFlow.collect должен быть умнее и мержить состояния.

                // Упрощенный вариант: этот combine устанавливает базовое состояние.
                // activeTimerFlow.collect его модифицирует.
                _uiState.update { current -> // Обновляем существующее состояние
                    current.copy(
                        currentDate = getCurrentDateStringViewModel(),
                        dailyTotalTimeFormatted = formatDurationViewModel(dailyTotalMillis, forceHours = true),
                        goals = goalsFromRepo,
                        activities = activitiesUi, // Устанавливаем базовые UI активности
                        isLoading = false,
                        errorMessage = null // Сбрасываем ошибку при успешной загрузке
                    )
                }
                // Возвращать здесь ничего не нужно, т.к. обновление идет через _uiState.update
            }.catch { throwable ->
                Log.e("HomeVM_Combine_Catch", "Error in combine for base data: ${throwable.message}", throwable)
                _uiState.value = MainScreenUiState(isLoading = false, errorMessage = throwable.message)
            }.launchIn(viewModelScope) // Используем launchIn, т.к. collect не нужен (обновляем через update)
        }
    }


    fun onActivityTimerToggle(activityStringId: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) { /* ... */ return@launch }
            val activity = activityRepository.getActivityById(activityStringId)
            if (activity == null) { /* ... */ return@launch }

            Log.i("HomeVM_Timer_Debug", "Toggling timer for UI ID: '$activityStringId'. Name='${activity.name}', LocalDB_ID=${activity.id}")
            val activityLongId = activity.id

            // Немедленное обновление UI для отзывчивости (isCurrentlyActive)
            // Окончательное состояние придет от activeTimerFlow и/или репозитория
            _uiState.update { current ->
                current.copy(
                    activities = current.activities.map {
                        if (it.baseActivity.id == activityLongId) it.copy(isCurrentlyActive = !it.isCurrentlyActive) // Оптимистичное переключение
                        else it
                    }
                )
            }

            try {
                // Проверяем состояние через TimerService, а не локально в ViewModel
                val activeTimerInfoCurrently = timerService.activeTimerFlow.value // Получаем текущее состояние таймера
                val isCurrentlyRunningForThisActivity = activeTimerInfoCurrently?.activityId == activityLongId

                if (isCurrentlyRunningForThisActivity) {
                    Log.d("HomeVM_Timer_Debug", "TimerService indicates active for $activityLongId. Stopping.")
                    timerService.stopTimer(activityLongId, userId)
                } else {
                    // Если какой-то другой таймер активен, TimerService должен его остановить,
                    // или ваша логика должна это учитывать (например, запрещать запуск нового).
                    // Предположим, TimerService.startTimer корректно обработает это (например, остановит предыдущий).
                    Log.d("HomeVM_Timer_Debug", "TimerService indicates NOT active for $activityLongId or active for another. Starting.")
                    timerService.startTimer(activityLongId, userId)
                }
            } catch (e: Exception) {
                Log.e("HomeVM_Timer_Debug", "Error toggling timer for LocalDB_ID $activityLongId: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Timer toggle error") }
            }
        }
    }

    fun onDeleteActivityClick(activityStringId: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.e("HomeVM", "Cannot delete activity: User ID is null.")
                return@launch
            }

            val activity = activityRepository.getActivityById(activityStringId)
            // Важно: используем activity.id (Long) для TimerService
            val activityLongId = activity?.id

            if (activityLongId != null) {
                try {
                    val activeSession = timerService.getActiveTimerSession(activityLongId, userId)
                    if (activeSession != null) {
                        Log.d("HomeVM", "Stopping active timer for activity $activityLongId before deletion.")
                        timerService.stopTimer(activityLongId, userId)
                    }
                } catch (e: Exception) {
                    Log.e("HomeVM", "Error stopping timer for activity $activityLongId during deletion: ${e.message}", e)
                    // Продолжаем удаление задачи, даже если остановка таймера не удалась
                }
            } else {
                Log.w("HomeVM", "Could not get Long ID for activity $activityStringId to check active session before deletion.")
            }

            // Удаляем задачу из репозитория по ее строковому ID
            try {
                activityRepository.deleteActivity(activityStringId)
                Log.d("HomeVM", "Activity $activityStringId deleted.")
            } catch (e: Exception) {
                Log.e("HomeVM", "Error deleting activity $activityStringId: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Could not delete activity.") }
            }
            // UI обновится автоматически через Flow от activityRepository
        }
    }

    // Функции-заглушки для навигации
    fun onDailyTimerClick() { Log.d("HomeVM", "Daily Timer Clicked - Navigate to Stats") }
    fun onAddActivityClick() { Log.d("HomeVM", "Add Activity Clicked - Navigate to AddEditActivity") }
    fun onEditGoalClick(goalId: String) { Log.d("HomeVM", "Edit Goal $goalId Clicked - Navigate to AddEditGoal") }
    fun onAddNewGoalClick() { Log.d("HomeVM", "Add New Goal Clicked - Navigate to AddEditGoal") }
    fun onViewAllGoalsClick() { Log.d("HomeVM", "View All Goals Clicked - Navigate to AllGoalsScreen") }

    // Вызывается, когда ViewModel очищается
    override fun onCleared() {
        super.onCleared()
        Log.d("HomeVM", "HomeViewModel cleared.")
    }
}