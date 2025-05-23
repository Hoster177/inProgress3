package ru.hoster.inprogress.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    // navController: NavController, // If needed for internal navigation
    onNavigateToAchievements: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToUserSettings: () -> Unit // Example for settings
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Профиль пользователя", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            // TODO: Implement avatar, status, and user details display (from Firebase)

            Text("Настройки приложения", style = MaterialTheme.typography.titleLarge)
            // TODO: Language toggle, Login/Logout, Notification settings, User Agreement
            Button(onClick = onNavigateToUserSettings) { Text("Общие настройки") } // Placeholder

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNavigateToAchievements) {
                Text("Достижения")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToHelp) {
                Text("Справка (FAQ)")
            }
            // TODO: Implement detailed sections for settings, achievements, help
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen({}, {}, {})
    }
}
