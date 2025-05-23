package ru.hoster.inprogress.navigation

// Событие для навигации, которое должно обрабатываться один раз
sealed class AuthNavigationEvent {
    object NavigateToMain : AuthNavigationEvent()
    // object NavigateToSignUp : AuthNavigationEvent() // Если нужно из LoginScreen
    // object NavigateToLogin : AuthNavigationEvent() // Если нужно из SignUpScreen
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null, // Для отображения ошибок пользователю
    val navigationEvent: AuthNavigationEvent? = null // Для одноразовых событий навигации
)