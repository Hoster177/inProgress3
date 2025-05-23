package ru.hoster.inprogress.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(
    navController: androidx.navigation.NavController
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("Русский") } // "Русский" or "English"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Общие настройки приложения", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            // Language Setting
            Text("Язык приложения", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedLanguage == "Русский", onClick = { selectedLanguage = "Русский" })
                Text("Русский", modifier = Modifier.padding(start = 8.dp))
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(selected = selectedLanguage == "English", onClick = { selectedLanguage = "English" })
                Text("English", modifier = Modifier.padding(start = 8.dp))
            }
            // TODO: Persist language choice and update app locale

            Spacer(modifier = Modifier.height(16.dp))

            // Notification Setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Уведомления", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
            }
            // TODO: Implement actual notification enabling/disabling logic

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { /* TODO: Implement Logout */ }) {
                Text("Выйти из аккаунта")
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { /* TODO: Show User Agreement */ }) {
                Text("Пользовательское соглашение")
            }
            // TODO: Implement login/logout, user agreement display, etc.
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserSettingsScreenPreview() {
    MaterialTheme {
        UserSettingsScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}
