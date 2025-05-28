package ru.hoster.inprogress.navigation.groups

import androidx.compose.foundation.Image // Для MemberItemRow, если используете аватары
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource // Для MemberItemRow (если используете placeholder)
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import ru.hoster.inprogress.R // Если используете placeholder аватар из drawable


// Assuming GroupDetailsViewModel, MemberDisplay, GroupDetailDisplay,
// GroupDetailsScreenUiState, GroupDetailsNavigationSignal are correctly defined
// and accessible (either in this file or imported).
// The ViewModel should already be providing todayTrackedTimeFormatted in MemberDisplay.

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
                // navController.navigate("edit_group_route/${signal.groupId}")
                println("Simulating navigation to edit group: ${signal.groupId}") // Placeholder
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
                            // TODO: Add more admin options if needed
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
                    if (group.groupCode != null && uiState.isCurrentUserAdmin) { // Show group code only to admin
                        Text("Код группы: ${group.groupCode}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                    uiState.error?.let { errorMsg ->
                        // Show error related to member loading if it exists and members are empty
                        if(uiState.members.isEmpty() && group.adminUserId.isNotBlank() && errorMsg.contains("members", ignoreCase = true)){
                            Text(
                                text = "Предупреждение: $errorMsg",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else if (!errorMsg.contains("members", ignoreCase = true) && !errorMsg.contains("Отображаются первые 30", ignoreCase = true)) {
                            // Show general errors not related to member loading or truncation
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
                        MemberItemRow( // This will now display the tracked time
                            member = member,
                            isCurrentUserAdmin = uiState.isCurrentUserAdmin,
                            currentUserId = uiState.currentUserId ?: ""
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Активность группы (TODO)", style = MaterialTheme.typography.titleLarge)
                    Text("Здесь будет лента активности группы или общие задачи.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else { // Fallback if group is null and not loading, and no specific error shown above
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
    avatarIdentifier: String?, // e.g., "avatar_bear", can be null
    displayName: String, // For content description
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultAvatarName = "avatar_default"

    // Determine the resource ID, defaulting to "avatar_default" if identifier is null, blank, or not found
    val resourceId = remember(avatarIdentifier) {
        val identifierToUse = if (avatarIdentifier.isNullOrBlank()) defaultAvatarName else avatarIdentifier
        var id = context.resources.getIdentifier(identifierToUse, "drawable", context.packageName)
        if (id == 0 && identifierToUse != defaultAvatarName) { // If specific avatar not found, try default
            id = context.resources.getIdentifier(defaultAvatarName, "drawable", context.packageName)
        }
        id // This will be 0 if even default is not found
    }

    if (resourceId != 0) {
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = "$displayName avatar",
            modifier = modifier.clip(CircleShape), // Ensure it's clipped
            contentScale = ContentScale.Crop
        )
    } else {
        // Absolute fallback (e.g., initials or a generic icon if even avatar_default.png is missing)
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                style = MaterialTheme.typography.bodyLarge, // Adjusted style for smaller avatar
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
        // Use the new MemberAvatar composable
        MemberAvatar(
            avatarIdentifier = member.avatarUrl, // Pass the avatarUrl from MemberDisplay
            displayName = member.displayName,
            modifier = Modifier
                .size(40.dp)
            // .clip(CircleShape) // clip is handled within MemberAvatar
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
            IconButton(onClick = { /* TODO: Показать опции управления участником */ }) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Управление участником")
            }
        }
    }
}