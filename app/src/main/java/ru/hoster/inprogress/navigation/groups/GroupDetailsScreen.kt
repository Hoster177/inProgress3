package ru.hoster.inprogress.navigation.groups

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController



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

                println("Simulating navigation to edit group: ${signal.groupId}")
                viewModel.onNavigationHandled()
            }
            null -> { /*  */ }
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
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Text("Загрузка деталей группы...", modifier = Modifier.padding(top = 70.dp))
            }
        } else if (uiState.error != null && uiState.group == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), contentAlignment = Alignment.Center) {
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
                    if (group.groupCode != null && uiState.isCurrentUserAdmin) {
                        Text("Код группы: ${group.groupCode}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                    uiState.error?.let { errorMsg ->

                        if(uiState.members.isEmpty() && group.adminUserId.isNotBlank() && errorMsg.contains("members", ignoreCase = true)){
                            Text(
                                text = "Предупреждение: $errorMsg",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else if (!errorMsg.contains("members", ignoreCase = true) && !errorMsg.contains("Отображаются первые 30", ignoreCase = true)) {

                            Text(
                                text = "Ошибка: $errorMsg",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Участники (${uiState.members.size})", style = MaterialTheme.typography.titleLarge)
                        Text("Время за сегодня", style = MaterialTheme.typography.titleSmall)
                    }
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
                            currentUserId = uiState.currentUserId ?: ""
                        )
                    }
                }


            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), contentAlignment = Alignment.Center) {
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
fun MemberAvatar(
    avatarIdentifier: String?,
    displayName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultAvatarName = "avatar_default"

    val resourceId = remember(avatarIdentifier) {
        val identifierToUse = if (avatarIdentifier.isNullOrBlank()) defaultAvatarName else avatarIdentifier
        var id = context.resources.getIdentifier(identifierToUse, "drawable", context.packageName)
        if (id == 0 && identifierToUse != defaultAvatarName) {
            id = context.resources.getIdentifier(defaultAvatarName, "drawable", context.packageName)
        }
        id
    }

    if (resourceId != 0) {
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = "$displayName avatar",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {

        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

        MemberAvatar(
            avatarIdentifier = member.avatarUrl,
            displayName = member.displayName,
            modifier = Modifier
                .size(40.dp)

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

        Text(
            text = member.todayTrackedTimeFormatted ?: "00:00:00",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        if (isCurrentUserAdmin && member.userId != currentUserId && member.userId.isNotBlank()) {
            IconButton(onClick = {  }) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Управление участником")
            }
        }
    }
}