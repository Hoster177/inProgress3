package ru.hoster.inprogress.navigation // Убедись, что пакет правильный

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
    val currentlyTimingDurationFormatted: String? = null, // Отформатированное время текущей сессии
    val currentlyTickingDurationMillis: Long = 0,
    val currentlyTimingActivityStartTime: Long? = null
) {

}

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
        // loadBaseData() // Можно убрать, если вся логика в combine ниже

        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) {
                _uiState.value = MainScreenUiState(isLoading = false, errorMessage = "Пользователь не авторизован")
                return@launch
            }

            // Подписка на тикающий таймер из TimerService (остается без изменений)
            timerService.activeTimerFlow
                //.distinctUntilChanged()
                .onEach { activeTimerInfo: ActiveTimerInfo? ->
                    _uiState.update { currentState ->
                        if (activeTimerInfo != null) {
                            Log.d("HomeVM_Ticker", "TimerService Tick: ActID=${activeTimerInfo.activityId}, Duration=${activeTimerInfo.currentDurationMillis}")
                            currentState.copy(
                                currentlyTimingActivityId = activeTimerInfo.activityId,
                                currentlyTimingActivityStartTime = activeTimerInfo.startTime,
                                currentlyTickingDurationMillis = activeTimerInfo.currentDurationMillis
                                // Не обновляем список activities здесь напрямую, это сделает combine ниже
                            )
                        } else {
                            Log.d("HomeVM_Ticker", "TimerService Tick: No active timer.")
                            currentState.copy(
                                currentlyTimingActivityId = null,
                                currentlyTimingActivityStartTime = null,
                                currentlyTickingDurationMillis = 0L
                            )
                        }
                    }
                }.launchIn(viewModelScope) // Запускаем этот Flow отдельно

            // Комбинированный Flow для обновления activitiesUi и dailyTotalTimeFormatted
            combine(
                activityRepository.getEnrichedActivitiesFlow(), // Используем новый метод
                _uiState.map { it.currentlyTimingActivityId }.distinctUntilChanged(),
                // _uiState.map { it.currentlyTimingActivityStartTime }.distinctUntilChanged(), // startTime не нужен для displayDuration, если есть tickingDuration
                _uiState.map { it.currentlyTickingDurationMillis }.distinctUntilChanged()
            ) { activitiesFromRepo, timingActivityId, tickingDuration ->

                Log.d("HomeVM_UI_Updater", "UI Updater: Repo activities: ${activitiesFromRepo.size}, TimingID: $timingActivityId, TickingDuration: $tickingDuration")

                val activitiesUi = activitiesFromRepo.map { repoActivity ->
                    // repoActivity.totalDurationMillisToday - это сумма завершенных сессий ЗА СЕГОДНЯ из репозитория
                    // repoActivity.isActive - активна ли сессия ЗА СЕГОДНЯ по данным репозитория (на момент последнего чтения БД)
                    var displayDurationToday = repoActivity.totalDurationMillisToday
                    val isActiveNowByTimer = repoActivity.id == timingActivityId // Активна ли СЕЙЧАС по данным TimerService

                    if (isActiveNowByTimer) {
                        // Если эта задача сейчас тикает, ее displayDuration - это сумма завершенных СЕГОДНЯ + текущая тикающая сессия
                        displayDurationToday += tickingDuration
                    }
                    // Если задача не тикает СЕЙЧАС (isActiveNowByTimer = false),
                    // то displayDurationToday остается суммой ее завершенных сессий за сегодня.

                    ActivityItemUi(
                        baseActivity = repoActivity,
                        displayDurationMillis = displayDurationToday,
                        displayDurationFormatted = formatDurationViewModel(displayDurationToday),
                        isCurrentlyActive = isActiveNowByTimer // Приоритет у TimerService для "isCurrentlyActive"
                    )
                }

                // dailyTotalTimeFormatted - это сумма displayDurationToday всех активностей
                val dailyTotalMillis = activitiesUi.sumOf { it.displayDurationMillis }

                // Обновляем цели отдельно, если они не зависят от этого combine
                // val currentGoals = _uiState.value.goals (если не хотим их перезагружать здесь)

                _uiState.update { current ->
                    current.copy(
                        currentDate = getCurrentDateStringViewModel(), // Обновляем дату на всякий случай
                        activities = activitiesUi,
                        dailyTotalTimeFormatted = formatDurationViewModel(dailyTotalMillis, forceHours = true),
                        isLoading = false,
                        errorMessage = null
                        // goals = currentGoals // или загружаем их здесь, если нужно
                    )
                }
            }.catch { throwable ->
                Log.e("HomeVM_UI_Updater", "Error in UI Updater combine: ${throwable.message}", throwable)
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
            }.launchIn(viewModelScope) // Запускаем и этот Flow

            // Загрузка целей (можно оставить здесь или объединить в один большой combine, если они зависят от userId)
            goalRepository.getActiveGoalsFlow() // Предполагается, что он внутренне использует userId
                .onEach { goals -> _uiState.update { it.copy(goals = goals) } }
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .launchIn(viewModelScope)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) { /* ... обработка ... */ return@launch }

            Log.d("HomeVM", "loadInitialData called for user: $userId")
            // Используем combine для получения базовых данных и данных о текущем таймере
            combine(
                activityRepository.getEnrichedActivitiesFlow(),
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

    private fun loadBaseData() { // Эта функция теперь может быть не нужна, если все в init
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) {
                _uiState.value = MainScreenUiState(isLoading = false, errorMessage = "User not logged in")
                return@launch
            }
            // Просто "запускаем" поток из репозитория.
            // combine в init его подхватит.
            // activityRepository.getActivitiesForTodayFlow().launchIn(viewModelScope) // Не обязательно, если combine уже есть
            // goalRepository.getActiveGoalsFlow().launchIn(viewModelScope) // Аналогично для целей

            // Можно добавить загрузку целей отдельно, если они не зависят от тикающего таймера
            goalRepository.getActiveGoalsFlow()
                .onEach { goals -> _uiState.update { it.copy(goals = goals) } }
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .launchIn(viewModelScope)
        }
    }

    fun onActivityTimerToggle(activityStringId: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) { /* ... обработка ... */ return@launch }

            val activity = activityRepository.getActivityById(activityStringId)
            if (activity == null) {
                Log.e("HomeVM_Timer_Toggle", "Activity NOT FOUND for stringId: $activityStringId")
                _uiState.update { it.copy(errorMessage = "Задача не найдена.") }
                return@launch
            }
            if (activity.firebaseId.isNullOrBlank()){
                Log.e("HomeVM_Timer_Toggle", "CRITICAL: FirebaseID is blank for activity '${activity.name}'. Cannot sync timer with Firestore.")
                _uiState.update { it.copy(errorMessage = "ID задачи для облака отсутствует. Таймер будет только локальным.") }
                // Можно решить, запускать ли таймер только локально или нет.
                // Для простоты, если firebaseId нет, не будем вызывать методы сервиса, которые требуют его для Firestore.
                // Или TimerService должен иметь версии методов без firebaseId для чисто локальной работы.
                // Пока что прервем, если firebaseId нужен.
                return@launch
            }

            val activityLocalId = activity.id
            val activityFirebaseId = activity.firebaseId // Уже проверили на isNullOrBlank

            Log.i("HomeVM_Timer_Toggle", "Toggling timer for UI ID: '$activityStringId'. Name='${activity.name}', LocalDB_ID=$activityLocalId, FirebaseID='$activityFirebaseId'")

            try {
                val activeTimerInfoCurrently = timerService.activeTimerFlow.value
                val isCurrentlyRunningForThisActivity = activeTimerInfoCurrently?.activityId == activityLocalId

                if (isCurrentlyRunningForThisActivity) {
                    Log.d("HomeVM_Timer_Toggle", "TimerService indicates active for $activityLocalId. Stopping.")
                    timerService.stopTimer(activityLocalId, userId, activityFirebaseId)
                } else {
                    Log.d("HomeVM_Timer_Toggle", "TimerService indicates NOT active for $activityLocalId or active for another. Starting.")
                    timerService.startTimer(activityLocalId, userId, activityFirebaseId)
                }
            } catch (e: Exception) {
                Log.e("HomeVM_Timer_Toggle", "Error toggling timer for LocalDB_ID $activityLocalId: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Ошибка переключения таймера") }
            }
        }
    }

    fun onDeleteActivityClick(activityStringId: String) { // activityStringId может быть firebaseId или строковым представлением localId
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.e("HomeVM_Delete", "Cannot delete activity: User ID is null.")
                _uiState.update { it.copy(errorMessage = "Пользователь не авторизован.") }
                return@launch
            }

            val activity = activityRepository.getActivityById(activityStringId)
            if (activity == null) {
                Log.w("HomeVM_Delete", "Activity with ID/FirebaseID '$activityStringId' not found for deletion.")
                _uiState.update { it.copy(errorMessage = "Задача для удаления не найдена.") }
                return@launch
            }

            val activityLocalId = activity.id
            val activityFirebaseId = activity.firebaseId // Получаем Firebase ID

            // Сначала останавливаем таймер, если он активен для этой задачи
            if (activityFirebaseId.isNullOrBlank()) {
                Log.w("HomeVM_Delete", "FirebaseID is blank for activity '${activity.name}' (LocalID: $activityLocalId). " +
                        "Cannot guarantee stopping timer in Firestore, but will attempt local stop.")
                // Если firebaseId нет, мы все равно можем попытаться остановить локальный таймер,
                // но синхронизация с Firestore для остановки сессии не произойдет.
                // В этом случае stopTimer вызовет ошибку, если он строго требует firebaseId.
                // Либо TimerService.stopTimer должен уметь обрабатывать опциональный firebaseId.
                // Для текущей реализации TimerService, где firebaseId обязателен для Firestore,
                // мы должны либо прервать, либо передать фиктивное значение, что плохо.
                // Лучше, если stopTimer сможет работать только с localId, если firebaseId отсутствует,
                // и тогда он просто не будет синхронизировать остановку с Firestore.

                // Пока предположим, что если firebaseId нет, то и сессий в Firestore для него нет.
                // Попытаемся остановить только локально, если TimerService это позволяет.
                // Однако, текущая сигнатура stopTimer требует firebaseId.
                // Это означает, что если у задачи нет firebaseId, мы не сможем корректно вызвать stopTimer.
            }

            try {
                // Проверяем активную сессию через TimerService, используя локальный ID
                val activeTimerInfoCurrently = timerService.activeTimerFlow.value
                val isTimerRunningForThisActivity = activeTimerInfoCurrently?.activityId == activityLocalId

                if (isTimerRunningForThisActivity) {
                    Log.d("HomeVM_Delete", "Timer is active for activity (LocalID: $activityLocalId, FirebaseID: '$activityFirebaseId'). Stopping it before deletion.")
                    if (!activityFirebaseId.isNullOrBlank()) {
                        timerService.stopTimer(activityLocalId, userId, activityFirebaseId)
                    } else {
                        // Если firebaseId нет, как мы должны остановить таймер в Firestore? Никак.
                        // Возможно, TimerService должен иметь версию stopTimer только с localId для локальной остановки.
                        // Или мы просто не останавливаем его в Firestore.
                        // Для простоты, если firebaseId нет, мы можем пропустить вызов stopTimer,
                        // или stopTimer должен быть достаточно умным.
                        // Пока что, если firebaseId нет, этот блок не будет выполнен для stopTimer.
                        Log.w("HomeVM_Delete", "Cannot stop timer in Firestore for LocalID $activityLocalId as FirebaseID is missing.")
                        // Можно попытаться остановить UI-таймер напрямую, если TimerService это позволяет без firebaseId
                        // timerService.forceStopLocalUiUpdaterFor(activityLocalId) // Гипотетический метод
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeVM_Delete", "Error stopping timer for activity (LocalID: $activityLocalId) during deletion: ${e.message}", e)
                // Продолжаем удаление задачи, даже если остановка таймера не удалась
            }

            // Удаляем задачу из репозитория
            try {
                activityRepository.deleteActivity(activityStringId) // Используем исходный activityStringId
                Log.i("HomeVM_Delete", "Activity (criteria: '$activityStringId', resolved LocalID: $activityLocalId, resolved FirebaseID: '$activityFirebaseId') request sent for deletion.")
                _uiState.update { it.copy(errorMessage = null) } // Очищаем предыдущие ошибки
            } catch (e: Exception) {
                Log.e("HomeVM_Delete", "Error deleting activity (criteria: '$activityStringId'): ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Не удалось удалить задачу.") }
            }
            // UI обновится автоматически через Flow от activityRepository, когда данные изменятся в Room.
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