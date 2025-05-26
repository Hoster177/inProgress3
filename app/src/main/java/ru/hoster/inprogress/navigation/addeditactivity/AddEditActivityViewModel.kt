package ru.hoster.inprogress.navigation.addeditactivity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Removed ActivityData import as it's not used and ActivityItem is preferred
// import ru.hoster.inprogress.domain.model.ActivityData
import ru.hoster.inprogress.domain.model.ActivityRepository
import ru.hoster.inprogress.domain.model.AuthService
// Removed Result import as it's not directly used by this ViewModel's public API after previous changes
// import ru.hoster.inprogress.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.hoster.inprogress.data.ActivityItem // Your ActivityItem class
import java.util.Date
import javax.inject.Inject

// Ensure predefinedColorsHex is accessible here (e.g., defined in this file or imported)


data class AddEditActivityScreenUiState(
    val activityName: String = "",
    val selectedColorHex: String? = predefinedColorsHex.firstOrNull(),
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val screenTitle: String = "Добавить занятие",
    val initialActivityLoaded: Boolean = false,
    val saveCompleted: Boolean = false,
    val error: String? = null,
    val loadedActivityItem: ActivityItem? = null
)

@HiltViewModel
class AddEditActivityViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val activityRepository: ActivityRepository,
    private val authService: AuthService
) : ViewModel() {

    private val activityId: String? = savedStateHandle.get<String>("activityId")

    private val _uiState = MutableStateFlow(AddEditActivityScreenUiState())
    val uiState: StateFlow<AddEditActivityScreenUiState> = _uiState.asStateFlow()

    init {
        val isEditingMode = activityId != null
        _uiState.update {
            it.copy(
                isEditing = isEditingMode,
                screenTitle = if (isEditingMode) "Редактировать занятие" else "Добавить занятие"
            )
        }
        if (isEditingMode && activityId != null) {
            loadActivity(activityId)
        } else {
            _uiState.update { it.copy(initialActivityLoaded = true, isLoading = false) }
        }
    }

    private fun loadActivity(id: String) {
        if (_uiState.value.initialActivityLoaded && _uiState.value.activityName.isNotEmpty() && _uiState.value.isEditing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val activityItem: ActivityItem? = activityRepository.getActivityById(id)
                if (activityItem != null) {
                    _uiState.update {
                        it.copy(
                            activityName = activityItem.name,
                            // Corrected line: Direct access, assuming ActivityItem has 'colorHex'
                            selectedColorHex = activityItem.colorHex ?: predefinedColorsHex.firstOrNull(),
                            loadedActivityItem = activityItem,
                            isLoading = false,
                            initialActivityLoaded = true
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Activity not found.", initialActivityLoaded = true) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load activity: ${e.message}",
                        initialActivityLoaded = true
                    )
                }
            }
        }
    }

    fun setActivityName(name: String) {
        _uiState.update { it.copy(activityName = name, error = null) }
    }

    fun setSelectedColor(colorHex: String?) {
        _uiState.update { it.copy(selectedColorHex = colorHex) }
    }

    fun saveActivity() {
        val currentName = _uiState.value.activityName
        if (currentName.isBlank()) {
            _uiState.update { it.copy(error = "Название не может быть пустым") }
            return
        }

        val currentUserId = authService.getCurrentUserId()
        if (currentUserId == null) {
            _uiState.update { it.copy(error = "User not authenticated.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val activityToSave: ActivityItem
                if (_uiState.value.isEditing && activityId != null) {
                    val existingItem = _uiState.value.loadedActivityItem
                        ?: activityRepository.getActivityById(activityId)

                    if (existingItem == null) {
                        _uiState.update { it.copy(isLoading = false, error = "Original activity not found for update.") }
                        return@launch
                    }
                    activityToSave = existingItem.copy(
                        name = currentName,
                        // Assuming ActivityItem has 'colorHex'. Update it if it exists.
                        colorHex = _uiState.value.selectedColorHex ?: existingItem.colorHex
                    )
                    activityRepository.updateActivity(activityToSave)
                } else {
                    activityToSave = ActivityItem(
                        firebaseId = null, // Will be set by backend or sync logic
                        userId = currentUserId,
                        name = currentName,
                        createdAt = Date(),
                        totalDurationMillisToday = 0L,
                        isActive = false,
                        // Assuming ActivityItem has 'colorHex'. Set it from UI state.
                        colorHex = _uiState.value.selectedColorHex
                    )
                    activityRepository.addActivity(activityToSave)
                }
                _uiState.update { it.copy(isLoading = false, saveCompleted = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to save activity: ${e.message}"
                    )
                }
            }
        }
    }

    fun onSaveCompletedHandled() {
        _uiState.update { it.copy(saveCompleted = false) }
    }
}