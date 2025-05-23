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
fun AddEditGoalScreen(
    navController: androidx.navigation.NavController,
    goalId: String? // Null if adding new, non-null if editing
) {
    val screenTitle = if (goalId == null) "Добавить цель" else "Редактировать цель"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(screenTitle, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            // TODO: Implement form fields for goal title, type, target, etc.
            // TODO: Load goal data if goalId is not null
            TextField(value = "", onValueChange = {}, label = { Text("Название цели") })
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* TODO: Save goal and pop back */ navController.popBackStack() }) {
                Text("Сохранить")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddGoalScreenPreview() {
    MaterialTheme {
        AddEditGoalScreen(navController = androidx.navigation.compose.rememberNavController(), goalId = null)
    }
}
