package ru.hoster.inprogress.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.hoster.inprogress.data.repository.FirestoreUserRepository
import ru.hoster.inprogress.domain.model.AuthService // Your actual interface
import ru.hoster.inprogress.domain.model.Result
import ru.hoster.inprogress.domain.model.UserData // Your actual UserData model
// Assuming FirestoreUserRepository has the specific updateUserProfile method
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authService: AuthService, // Using your actual AuthService interface
    private val userProfileRepository: FirestoreUserRepository // Using your actual FirestoreUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileScreenUiState())
    val uiState: StateFlow<ProfileScreenUiState> = _uiState.asStateFlow()

    // These are identifiers for your drawable resources (e.g., "avatar_1", "avatar_2")
    val predefinedAvatars: List<String> = listOf("avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_default")

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = authService.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Пользователь не авторизован.") }
                return@launch
            }

            // Fetch from FirestoreUserRepository which should return Result<UserData?>
            when (val profileResult = userProfileRepository.getUserById(userId)) {
                is Result.Success -> {
                    val profile = profileResult.data
                    val email = authService.getCurrentUserEmail()

                    if (profile != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentName = profile.displayName, // Corrected: Use displayName
                                editableName = profile.displayName, // Corrected: Use displayName
                                // Use avatarUrl, provide default if empty or null
                                currentAvatarId = profile.avatarUrl?.ifEmpty { predefinedAvatars.last() } ?: predefinedAvatars.last(),
                                selectedAvatarId = profile.avatarUrl?.ifEmpty { predefinedAvatars.last() } ?: predefinedAvatars.last(),
                                email = email ?: profile.email ?: "" // Prioritize auth service email, then profile email
                            )
                        }
                    } else {
                        // Profile not found, but user is authenticated. Maybe create one or use defaults.
                        // For now, show defaults and allow user to set them.
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentName = "Пользователь", // Default name
                                editableName = "Пользователь", // Default name
                                currentAvatarId = predefinedAvatars.last(),
                                selectedAvatarId = predefinedAvatars.last(),
                                email = email ?: ""
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = "Не удалось загрузить профиль: ${profileResult.message.localizedMessage}") }
                }
            }
        }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(editableName = newName) }
    }

    fun onAvatarSelected(avatarId: String) {
        // avatarId here is the identifier like "avatar_1", which will be stored as avatarUrl
        _uiState.update { it.copy(selectedAvatarId = avatarId) }
    }

    fun saveProfileChanges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val userId = authService.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, error = "Пользователь не авторизован.") }
                return@launch
            }

            // Use the specific method from FirestoreUserRepository
            val success = userProfileRepository.updateUserProfile(
                userId = userId,
                name = _uiState.value.editableName, // This will be saved as displayName in Firestore
                avatarId = _uiState.value.selectedAvatarId // This will be saved as avatarUrl in Firestore
            )

            if (success) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        currentName = it.editableName,
                        currentAvatarId = it.selectedAvatarId,
                        saveSuccessMessage = "Профиль успешно обновлен!"
                    )
                }
            } else {
                _uiState.update { it.copy(isSaving = false, error = "Не удалось сохранить изменения.") }
            }
        }
    }

    fun clearSaveSuccessMessage() {
        _uiState.update { it.copy(saveSuccessMessage = null) }
    }
}

// This UI state remains the same as it's for the screen's presentation
data class ProfileScreenUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccessMessage: String? = null,
    val currentName: String = "", // Represents displayName from UserData
    val editableName: String = "", // Represents displayName from UserData
    val email: String = "",
    val currentAvatarId: String = "avatar_default", // Represents avatarUrl from UserData (or your identifier)
    val selectedAvatarId: String = "avatar_default" // Represents avatarUrl from UserData (or your identifier)
)