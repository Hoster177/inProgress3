package ru.hoster.inprogress.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpFaqScreen(
    navController: androidx.navigation.NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Справка (FAQ)") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                Text("Часто задаваемые вопросы", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
            }
            // TODO: Populate with actual FAQ items
            item { QuestionAnswer("Как добавить задачу?", "На главном экране нажмите кнопку '+' внизу.") }
            item { QuestionAnswer("Как работает таймер?", "Таймер продолжает работать в фоновом режиме, если вы свернете приложение.") }
            item { QuestionAnswer("Что такое группы?", "Группы позволяют соревноваться с друзьями и видеть их прогресс.") }
        }
    }
}

@Composable
fun QuestionAnswer(question: String, answer: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(question, style = MaterialTheme.typography.titleMedium)
        Text(answer, style = MaterialTheme.typography.bodyMedium)
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun HelpFaqScreenPreview() {
    MaterialTheme {
        HelpFaqScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}