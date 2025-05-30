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
import ru.hoster.inprogress.domain.model.Result // Your Result class
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

                        it.copy(isLoading = false, errorMessage = mapAuthException(result.message))
                    }
                }

            }
        }
    }

    fun signUp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authService.signUp(uiState.value.email, uiState.value.password)
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, navigationEvent = AuthNavigationEvent.NavigateToMain)
                    }
                }
                is Result.Error -> {
                    _uiState.update {

                        it.copy(isLoading = false, errorMessage = mapAuthException(result.message))
                    }
                }

            }
        }
    }

    fun onNavigationEventConsumed() {
        _uiState.update { it.copy(navigationEvent = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun mapAuthException(exception: Exception): String {
        return when (exception) {
            is com.google.firebase.FirebaseNetworkException -> "Проверьте интернет-соединение"
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "Пароль слишком слабый. Используйте не менее 6 символов."
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Неверный email или пароль."
            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Пользователь с таким email уже существует."
            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Пользователь с таким email не найден."
            else -> exception.message ?: "Произошла неизвестная ошибка"
        }
    }
}