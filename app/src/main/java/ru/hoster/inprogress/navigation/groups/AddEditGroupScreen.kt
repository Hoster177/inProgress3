package ru.hoster.inprogress.navigation.groups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGroupScreen(
    navController: NavController,
    groupId: String?
    // viewModel: AddEditGroupViewModel = hiltViewModel()
) {
    // LaunchedEffect(Unit) {
    //     if (groupId != null) {
    //         // viewModel.loadGroup(groupId) // Если редактируем существующую группу
    //     }
    // }
    // val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (groupId == null) "Создать группу" else "Редактировать группу") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
                // TODO: Добавить кнопку "Сохранить"
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (groupId == null) "Экран Создания Группы (Заглушка)"
                    else "Экран Редактирования Группы (ID: $groupId) (Заглушка)"
                )
                // TODO: Добавить поля для названия группы, описания и т.д.

            }
        }
    }
}
