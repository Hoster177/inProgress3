package ru.hoster.inprogress.navigation


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




data class ActivityItemUi(
    val baseActivity: ActivityItem,
    val displayDurationMillis: Long,
    val displayDurationFormatted: String,
    val isCurrentlyActive: Boolean
)

data class MainScreenUiState(
    val currentDate: String = getCurrentDateStringViewModel(),
    val dailyTotalTimeFormatted: String = "00:00:00",
    val goals: List<Goal> = emptyList(),
    val activities: List<ActivityItemUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentlyTimingActivityId: Long? = null,
    val currentlyTimingDurationFormatted: String? = null,
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


        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) {
                _uiState.value = MainScreenUiState(isLoading = false, errorMessage = "Пользователь не авторизован")
                return@launch
            }


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
                }.launchIn(viewModelScope)


            combine(
                activityRepository.getEnrichedActivitiesFlow(),
                _uiState.map { it.currentlyTimingActivityId }.distinctUntilChanged(),
                //
                _uiState.map { it.currentlyTickingDurationMillis }.distinctUntilChanged()
            ) { activitiesFromRepo, timingActivityId, tickingDuration ->

                Log.d("HomeVM_UI_Updater", "UI Updater: Repo activities: ${activitiesFromRepo.size}, TimingID: $timingActivityId, TickingDuration: $tickingDuration")

                val activitiesUi = activitiesFromRepo.map { repoActivity ->
                    var displayDurationToday = repoActivity.totalDurationMillisToday
                    val isActiveNowByTimer = repoActivity.id == timingActivityId

                    if (isActiveNowByTimer) {
                        displayDurationToday += tickingDuration
                    }


                    ActivityItemUi(
                        baseActivity = repoActivity,
                        displayDurationMillis = displayDurationToday,
                        displayDurationFormatted = formatDurationViewModel(displayDurationToday),
                        isCurrentlyActive = isActiveNowByTimer
                    )
                }


                val dailyTotalMillis = activitiesUi.sumOf { it.displayDurationMillis }


                _uiState.update { current ->
                    current.copy(
                        currentDate = getCurrentDateStringViewModel(),
                        activities = activitiesUi,
                        dailyTotalTimeFormatted = formatDurationViewModel(dailyTotalMillis, forceHours = true),
                        isLoading = false,
                        errorMessage = null

                    )
                }
            }.catch { throwable ->
                Log.e("HomeVM_UI_Updater", "Error in UI Updater combine: ${throwable.message}", throwable)
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
            }.launchIn(viewModelScope)


            goalRepository.getActiveGoalsFlow()
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

            combine(
                activityRepository.getEnrichedActivitiesFlow(),
                goalRepository.getActiveGoalsFlow()

            ) { activitiesFromRepo, goalsFromRepo ->
                Log.i("HomeVM_Combine", "Base data received. Activities: ${activitiesFromRepo.size}, Goals: ${goalsFromRepo.size}")


                val activitiesUi = activitiesFromRepo.map { repoActivity ->
                    ActivityItemUi(
                        baseActivity = repoActivity,
                        displayDurationMillis = repoActivity.totalDurationMillisToday,
                        displayDurationFormatted = formatDurationViewModel(repoActivity.totalDurationMillisToday),
                        isCurrentlyActive = repoActivity.isActive
                    )
                }

                val dailyTotalMillis = activitiesUi.sumOf { it.displayDurationMillis }


                _uiState.update { current ->
                    current.copy(
                        currentDate = getCurrentDateStringViewModel(),
                        dailyTotalTimeFormatted = formatDurationViewModel(dailyTotalMillis, forceHours = true),
                        goals = goalsFromRepo,
                        activities = activitiesUi,
                        isLoading = false,
                        errorMessage = null
                    )
                }

            }.catch { throwable ->
                Log.e("HomeVM_Combine_Catch", "Error in combine for base data: ${throwable.message}", throwable)
                _uiState.value = MainScreenUiState(isLoading = false, errorMessage = throwable.message)
            }.launchIn(viewModelScope)
        }
    }

    private fun loadBaseData() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) {
                _uiState.value = MainScreenUiState(isLoading = false, errorMessage = "User not logged in")
                return@launch
            }

            goalRepository.getActiveGoalsFlow()
                .onEach { goals -> _uiState.update { it.copy(goals = goals) } }
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .launchIn(viewModelScope)
        }
    }

    fun onActivityTimerToggle(activityStringId: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) {  return@launch }

            val activity = activityRepository.getActivityById(activityStringId)
            if (activity == null) {
                Log.e("HomeVM_Timer_Toggle", "Activity NOT FOUND for stringId: $activityStringId")
                _uiState.update { it.copy(errorMessage = "Задача не найдена.") }
                return@launch
            }
            if (activity.firebaseId.isNullOrBlank()){
                Log.e("HomeVM_Timer_Toggle", "CRITICAL: FirebaseID is blank for activity '${activity.name}'. Cannot sync timer with Firestore.")
                _uiState.update { it.copy(errorMessage = "ID задачи для облака отсутствует. Таймер будет только локальным.") }

                return@launch
            }

            val activityLocalId = activity.id
            val activityFirebaseId = activity.firebaseId

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

    fun onDeleteActivityClick(activityStringId: String) {
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
            val activityFirebaseId = activity.firebaseId

            if (activityFirebaseId.isNullOrBlank()) {
                Log.w("HomeVM_Delete", "FirebaseID is blank for activity '${activity.name}' (LocalID: $activityLocalId). " +
                        "Cannot guarantee stopping timer in Firestore, but will attempt local stop.")

            }

            try {

                val activeTimerInfoCurrently = timerService.activeTimerFlow.value
                val isTimerRunningForThisActivity = activeTimerInfoCurrently?.activityId == activityLocalId

                if (isTimerRunningForThisActivity) {
                    Log.d("HomeVM_Delete", "Timer is active for activity (LocalID: $activityLocalId, FirebaseID: '$activityFirebaseId'). Stopping it before deletion.")
                    if (!activityFirebaseId.isNullOrBlank()) {
                        timerService.stopTimer(activityLocalId, userId, activityFirebaseId)
                    } else {

                        Log.w("HomeVM_Delete", "Cannot stop timer in Firestore for LocalID $activityLocalId as FirebaseID is missing.")

                    }
                }
            } catch (e: Exception) {
                Log.e("HomeVM_Delete", "Error stopping timer for activity (LocalID: $activityLocalId) during deletion: ${e.message}", e)

            }

            try {
                activityRepository.deleteActivity(activityStringId)
                Log.i("HomeVM_Delete", "Activity (criteria: '$activityStringId', resolved LocalID: $activityLocalId, resolved FirebaseID: '$activityFirebaseId') request sent for deletion.")
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                Log.e("HomeVM_Delete", "Error deleting activity (criteria: '$activityStringId'): ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Не удалось удалить задачу.") }
            }

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