package ru.hoster.inprogress.navigation // или ваш пакет

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ru.hoster.inprogress.navigation.AuthNavigationEvent
import ru.hoster.inprogress.navigation.AuthViewModel
import ru.hoster.inprogress.navigation.AuthUiState // Убедитесь, что импорт правильный

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel() // Внедряем ViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current // Для Toast или Snackbar, если нужно

    // Обработка навигационных событий
    LaunchedEffect(key1 = uiState.navigationEvent) {
        when (val event = uiState.navigationEvent) {
            is AuthNavigationEvent.NavigateToMain -> {
                navController.navigate(Screen.Main.route) { // Переход на главный экран
                    popUpTo(Route.LOGIN) { inclusive = true } // Удаляем LoginScreen из бэкстека
                }
                viewModel.onNavigationEventConsumed() // Сообщаем ViewModel, что событие обработано
            }
            null -> { /* No event */ }
        }
    }

    // Обработка сообщений об ошибках (например, через Snackbar)
    LaunchedEffect(key1 = uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            // Здесь можно показать Snackbar
            // scaffoldState.snackbarHostState.showSnackbar(message)
            // Пока просто выведем в Log или можно использовать Toast для простоты
            android.util.Log.e("LoginScreen", "Error: $message")
            // Не забудьте вызвать viewModel.clearErrorMessage() если показываете Snackbar/Toast и хотите
            // чтобы он не показывался снова при рекомпозиции без нового события ошибки.
            // Для простоты, пока можно оставить так, ошибка будет видна в errorMessage Text.
        }
    }

    Scaffold( // Используем Scaffold для возможности добавления Snackbar в будущем
        // snackbarHost = { SnackbarHost(hostState = scaffoldState.snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Вход", style = MaterialTheme.typography.headlineMedium)

                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    label = { Text("Пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = { viewModel.signIn() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.email.isNotBlank() && uiState.password.isNotBlank() // Кнопка активна, если поля не пустые
                    ) {
                        Text("Войти")
                    }
                }

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                TextButton(onClick = {
                    navController.navigate(Route.SIGN_UP) // Переход на экран регистрации
                }) {
                    Text("Нет аккаунта? Зарегистрироваться")
                }

                // Кнопка для симуляции входа (если еще нужна для тестирования без Firebase)
                 Button(onClick = {
                     navController.navigate(Screen.Main.route) {
                         popUpTo(Route.LOGIN) { inclusive = true }
                     }
                 }) {
                     Text("Войти (Симуляция)")
                 }
            }
        }
    }
}