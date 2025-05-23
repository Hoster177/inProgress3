package ru.hoster.inprogress.navigation.addeditactivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.hoster.inprogress.domain.model.ActivityData
import ru.hoster.inprogress.domain.model.ActivityRepository
import ru.hoster.inprogress.domain.model.AuthService
import ru.hoster.inprogress.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

// UI State and Navigation Signal (can be in this file or a separate ui.model file)
data class AddEditActivityScreenUiState(
    val activityName: String = "",
    val selectedColorHex: String? = predefinedColorsHex.firstOrNull(),
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val screenTitle: String = "Добавить занятие",
    val initialActivityLoaded: Boolean = false,
    val saveCompleted: Boolean = false,
    val error: String? = null
)

// Predefined colors list - ensure this is accessible.
// It was previously in AddEditActivityScreen.kt.
// For simplicity, I'll assume it's available here or you can move it to a common constants file.
// val predefinedColorsHex = listOf("#FF6B6B", ...)

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
            _uiState.update { it.copy(initialActivityLoaded = true) } // Ready for new entry
        }
    }

    private fun loadActivity(id: String) {
        if (_uiState.value.initialActivityLoaded && _uiState.value.isEditing) return // Avoid re-loading if already loaded for edit

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = activityRepository.getActivityById(id)) {
                is Result.Success -> {
                    val activityData = result.data
                    if (activityData != null) {
                        _uiState.update {
                            it.copy(
                                activityName = activityData.name,
                                selectedColorHex = activityData.colorHex ?: predefinedColorsHex.firstOrNull(),
                                isLoading = false,
                                initialActivityLoaded = true
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Activity not found.", initialActivityLoaded = true) }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load activity: ${result.exception.message}",
                            initialActivityLoaded = true
                        )
                    }
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

            val activityToSave: ActivityData = if (_uiState.value.isEditing && activityId != null) {
                // For update, fetch existing to preserve fields not directly edited on this screen
                // This is a simplified update; a real one might be more complex
                val existingActivity = (activityRepository.getActivityById(activityId) as? Result.Success)?.data
                (existingActivity ?: ActivityData(id = activityId, userId = currentUserId)).copy(
                    name = currentName,
                    colorHex = _uiState.value.selectedColorHex
                    // Preserve other fields like isActive, totalDurationMillisToday, lastStartTime, createdAt
                )
            } else {
                ActivityData(
                    // id will be generated by repo or backend if empty
                    userId = currentUserId,
                    name = currentName,
                    colorHex = _uiState.value.selectedColorHex,
                    createdAt = Date()
                )
            }

            val result: Result<*> = if (_uiState.value.isEditing) {
                activityRepository.updateActivity(activityToSave)
            } else {
                activityRepository.insertActivity(activityToSave)
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, saveCompleted = true) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to save activity: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }

    fun onSaveCompletedHandled() {
        _uiState.update { it.copy(saveCompleted = false) } // Reset signal
    }
}