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
fun SignUpScreen(
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
                    // Очищаем бэкстек до Login (если он был), и сам Login, и SignUp
                    popUpTo(Route.LOGIN) { inclusive = true }
                    // Если бы мы хотели вернуться на Login после регистрации, то было бы:
                    // popUpTo(Route.SIGN_UP) { inclusive = true }
                    // navController.navigate(Route.LOGIN)
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
            android.util.Log.e("SignUpScreen", "Error: $message")
            // viewModel.clearErrorMessage() // Если нужно сбрасывать после показа
        }
    }

    Scaffold { paddingValues ->
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
                Text("Регистрация", style = MaterialTheme.typography.headlineMedium)

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
                    label = { Text("Пароль (мин. 6 символов)") }, // Firebase требует минимум 6 символов
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                // Можно добавить поле для подтверждения пароля, если требуется
                // OutlinedTextField(
                //     value = confirmPassword,
                //     onValueChange = { confirmPassword = it },
                //     label = { Text("Подтвердите пароль") },
                //     visualTransformation = PasswordVisualTransformation(),
                //     modifier = Modifier.fillMaxWidth()
                // )

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = { viewModel.signUp() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.email.isNotBlank() && uiState.password.length >= 6 // Проверка длины пароля
                    ) {
                        Text("Зарегистрироваться")
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
                    navController.popBackStack() // Возврат на предыдущий экран (LoginScreen)
                }) {
                    Text("Уже есть аккаунт? Войти")
                }
            }
        }
    }
}