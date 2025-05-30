package ru.hoster.inprogress.navigation


sealed class AuthNavigationEvent {
    object NavigateToMain : AuthNavigationEvent()
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigationEvent: AuthNavigationEvent? = null
)