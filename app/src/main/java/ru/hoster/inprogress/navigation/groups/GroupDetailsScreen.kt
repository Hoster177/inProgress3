package ru.hoster.inprogress.navigation.groups

import androidx.compose.foundation.Image // Для MemberItemRow, если используете аватары
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape // Для MemberItemRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings // Для MemberItemRow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // Для MemberItemRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale // Для MemberItemRow
import androidx.compose.ui.res.painterResource // Для MemberItemRow (если используете placeholder)
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import ru.hoster.inprogress.R // Если используете placeholder аватар из drawable

// Если GroupDetailsNavigationSignal и другие UI модели определены в GroupDetailsViewModel.kt,
// то отдельные импорты для них здесь не нужны.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    navController: NavController,
    viewModel: GroupDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showOptionsMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.navigationSignal) {
        when (val signal = uiState.navigationSignal) {
            is GroupDetailsNavigationSignal.NavigateBack -> {
                navController.popBackStack()
                viewModel.onNavigationHandled()
            }
            is GroupDetailsNavigationSignal.NavigateToEditGroup -> {
                // TODO: Реализовать навигацию на экран редактирования группы
                // navController.navigate("edit_group_route/${signal.groupId}")
                println("Simulating navigation to edit group: ${signal.groupId}")
                viewModel.onNavigationHandled()
            }
            null -> { /* Do nothing */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.group?.name ?: "Группа") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (uiState.group != null) {
                        if (uiState.isCurrentUserAdmin) {
                            IconButton(onClick = { viewModel.navigateToEditGroup() }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Редактировать группу")
                            }
                        }
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Опции")
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Покинуть группу") },
                                onClick = {
                                    viewModel.confirmLeaveGroup(true)
                                    showOptionsMenu = false
                                }
                            )
                            // TODO: Добавить больше опций для администратора (например, управление участниками, удаление группы)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Text("Загрузка деталей группы...", modifier = Modifier.padding(top = 70.dp))
            }
        } else if (uiState.error != null && uiState.group == null) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Не удалось загрузить информацию о группе.", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (uiState.group != null) {
            val group = uiState.group!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(group.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (!group.description.isNullOrBlank()) {
                        Text(group.description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                    }
                    if (group.groupCode != null) {
                        Text("Код группы: ${group.groupCode}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                    uiState.error?.let {
                        if(uiState.members.isEmpty() && group.adminUserId.isNotBlank()){
                            Text(
                                text = "Предупреждение: $it",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                }

                item {
                    Text("Участники (${uiState.members.size})", style = MaterialTheme.typography.titleLarge)
                }
                if (uiState.members.isEmpty() && !uiState.isLoading && group.adminUserId.isNotBlank()) {
                    item {
                        Text(
                            if(uiState.error?.contains("members") == true) "Не удалось загрузить участников."
                            else "В этой группе пока нет других участников.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(uiState.members, key = { it.userId }) { member ->
                        MemberItemRow(
                            member = member,
                            isCurrentUserAdmin = uiState.isCurrentUserAdmin,
                            currentUserId = uiState.currentUserId ?: "" // <--- ИСПОЛЬЗУЕМ ИЗ uiState
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Активность группы (TODO)", style = MaterialTheme.typography.titleLarge)
                    Text("Здесь будет лента активности группы или общие задачи.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Информация о группе недоступна.", style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (uiState.showLeaveGroupDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.confirmLeaveGroup(false) },
                title = { Text("Покинуть группу?") },
                text = { Text("Вы уверены, что хотите покинуть группу \"${uiState.group?.name ?: ""}\"?") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.leaveGroup() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        if (uiState.leaveGroupInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Покинуть")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.confirmLeaveGroup(false) }) { Text("Отмена") }
                }
            )
        }
    }
}

@Composable
fun MemberItemRow(member: MemberDisplay, isCurrentUserAdmin: Boolean, currentUserId: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            // painter = rememberAsyncImagePainter(model = member.avatarUrl, placeholder = painterResource(id = R.drawable.ic_placeholder_avatar)), // Пример с Coil
            painter = painterResource(id = R.drawable.baseline_account_circle_24), // Заглушка
            contentDescription = "${member.displayName} avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (member.userId == currentUserId) "${member.displayName} (Вы)" else member.displayName,
                style = MaterialTheme.typography.bodyLarge
            )
            if (member.isAdmin) {
                Text("Администратор", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (isCurrentUserAdmin && member.userId != currentUserId && member.userId.isNotBlank()) { // Добавил member.userId.isNotBlank() для надежности
            IconButton(onClick = { /* TODO: Показать опции управления участником (удалить, сделать админом и т.д.) */ }) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Управление участником")
            }
        }
    }
}