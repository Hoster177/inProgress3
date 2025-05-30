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
import ru.hoster.inprogress.domain.model.AuthService
import ru.hoster.inprogress.domain.model.Result
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authService: AuthService,
    private val userProfileRepository: FirestoreUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileScreenUiState())
    val uiState: StateFlow<ProfileScreenUiState> = _uiState.asStateFlow()


    val predefinedAvatars: List<String> = listOf("avatar_bear", "avatar_cat", "avatar_dog", "avatar_rabbit", "avatar_default")

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


            when (val profileResult = userProfileRepository.getUserById(userId)) {
                is Result.Success -> {
                    val profile = profileResult.data
                    val email = authService.getCurrentUserEmail()

                    if (profile != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentName = profile.displayName,
                                editableName = profile.displayName,
                                currentAvatarId = profile.avatarUrl?.ifEmpty { predefinedAvatars.last() } ?: predefinedAvatars.last(),
                                selectedAvatarId = profile.avatarUrl?.ifEmpty { predefinedAvatars.last() } ?: predefinedAvatars.last(),
                                email = email ?: profile.email ?: ""
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentName = "Пользователь",
                                editableName = "Пользователь",
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


            val success = userProfileRepository.updateUserProfile(
                userId = userId,
                name = _uiState.value.editableName,
                avatarId = _uiState.value.selectedAvatarId
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

data class ProfileScreenUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccessMessage: String? = null,
    val currentName: String = "",
    val editableName: String = "",
    val email: String = "",
    val currentAvatarId: String = "avatar_default",
    val selectedAvatarId: String = "avatar_default"
)