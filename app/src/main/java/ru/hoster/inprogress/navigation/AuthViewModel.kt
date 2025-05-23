package ru.hoster.inprogress.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.hoster.inprogress.domain.model.AuthService
import ru.hoster.inprogress.domain.model.Result // Ваш Result класс
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun signIn() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authService.signIn(uiState.value.email, uiState.value.password)
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, navigationEvent = AuthNavigationEvent.NavigateToMain)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = mapAuthException(result.exception))
                    }
                }
            }
        }
    }

    fun signUp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // Здесь authService.signUp() уже должен вызывать userRepository.createUserProfile() внутри себя
            val result = authService.signUp(uiState.value.email, uiState.value.password)
            when (result) {
                is Result.Success -> {
                    // result.data содержит userId, если он нужен здесь
                    _uiState.update {
                        it.copy(isLoading = false, navigationEvent = AuthNavigationEvent.NavigateToMain)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = mapAuthException(result.exception))
                    }
                }
            }
        }
    }

    // Вызывается из UI после обработки навигационного события
    fun onNavigationEventConsumed() {
        _uiState.update { it.copy(navigationEvent = null) }
    }

    // Вызывается из UI для сброса сообщения об ошибке, если нужно
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun mapAuthException(exception: Exception): String {
        // Здесь можно добавить более специфичную обработку ошибок Firebase
        // com.google.firebase.FirebaseNetworkException -> "Проверьте интернет-соединение"
        // com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "Пароль слишком слабый. Используйте не менее 6 символов."
        // com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Неверный email или пароль."
        // com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Пользователь с таким email уже существует."
        // com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Пользователь с таким email не найден."
        return exception.message ?: "Произошла неизвестная ошибка"
    }
}