package ru.hoster.inprogress.navigation.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel = hiltViewModel(),
    onNavigateToGroupDetails: (groupId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var groupCodeInput by remember { mutableStateOf("") }
    var newGroupNameInput by remember { mutableStateOf("") }
    var newGroupDescriptionInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiState.collectLatest { state ->
            if (state.actionSuccessMessage != null) {
                snackbarHostState.showSnackbar(
                    message = state.actionSuccessMessage,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearActionSuccessMessage()
            }
            if (state.error != null) {
                snackbarHostState.showSnackbar(
                    message = "Ошибка: ${state.error}",
                    duration = SnackbarDuration.Short
                )
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Мои Группы") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FloatingActionButton(
                    onClick = { viewModel.showJoinGroupDialog(true) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Filled.GroupAdd, contentDescription = "Присоединиться к группе")
                }
                FloatingActionButton(
                    onClick = { viewModel.showCreateGroupDialog(true) }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Создать группу")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Загрузка групп...", modifier = Modifier.padding(top = 60.dp))
                }
            } else if (uiState.userGroups.isEmpty() && uiState.error == null) { // Show empty state only if no error
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Вы еще не состоите ни в одной группе. Создайте новую или присоединитесь к существующей!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.userGroups, key = { it.id }) { group ->
                        GroupItemCard(
                            group = group,
                            onClick = { onNavigateToGroupDetails(group.id) }
                        )
                    }
                }
            }
        }

        // --- Dialog for Joining a Group ---
        if (uiState.joinGroupDialogVisible) {
            AlertDialog(
                onDismissRequest = { if (!uiState.isProcessingAction) viewModel.showJoinGroupDialog(false); groupCodeInput = "" },
                title = { Text("Присоединиться к группе") },
                text = {
                    OutlinedTextField(
                        value = groupCodeInput,
                        onValueChange = { groupCodeInput = it },
                        label = { Text("Код группы") },
                        singleLine = true,
                        enabled = !uiState.isProcessingAction
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.joinGroup(groupCodeInput.trim().uppercase()) },
                        enabled = !uiState.isProcessingAction && groupCodeInput.isNotBlank()
                    ) {
                        if (uiState.isProcessingAction) CircularProgressIndicator(Modifier.size(24.dp)) else Text("Присоединиться")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.showJoinGroupDialog(false); groupCodeInput = "" },
                        enabled = !uiState.isProcessingAction
                    ) { Text("Отмена") }
                }
            )
        }

        // --- Dialog for Creating a Group ---
        if (uiState.createGroupDialogVisible) {
            AlertDialog(
                onDismissRequest = {
                    if (!uiState.isProcessingAction) viewModel.showCreateGroupDialog(false)
                    newGroupNameInput = ""
                    newGroupDescriptionInput = ""
                },
                title = { Text("Создать новую группу") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newGroupNameInput,
                            onValueChange = { newGroupNameInput = it },
                            label = { Text("Название группы") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isProcessingAction
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newGroupDescriptionInput,
                            onValueChange = { newGroupDescriptionInput = it },
                            label = { Text("Описание (необязательно)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            enabled = !uiState.isProcessingAction
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.createGroup(newGroupNameInput.trim(), newGroupDescriptionInput.trim()) },
                        enabled = !uiState.isProcessingAction && newGroupNameInput.isNotBlank()
                    ) {
                        if (uiState.isProcessingAction) CircularProgressIndicator(Modifier.size(24.dp)) else Text("Создать")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.showCreateGroupDialog(false)
                            newGroupNameInput = ""
                            newGroupDescriptionInput = ""
                        },
                        enabled = !uiState.isProcessingAction
                    ) { Text("Отмена") }
                }
            )
        }
    }
}

@Composable
fun GroupItemCard(group: GroupPreview, onClick: () -> Unit) { // Definition is the same as your stub
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = group.description ?: "Нет описания",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Участников: ${group.memberCount}",
                    style = MaterialTheme.typography.bodySmall
                )
//                if (group.lastActivity != null) {
//                    Text(
//                        "Активность: ${group.lastActivity}",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "Перейти к группе",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// --- Previews ---
//@Preview(showBackground = true, name = "Groups Screen with Data")
//@Composable
//fun GroupsScreenWithDataPreview() {
//    MaterialTheme {
//        // For preview, we'd ideally pass a mock ViewModel or a UiState directly
//        // This direct call will use a hiltViewModel if Hilt is setup for previews,
//        // or fail if not. For simple UI previews, direct state passing is better.
//        GroupsScreen(
//            viewModel = MockGroupsViewModel(), // Example of using a mock for preview
//            onNavigateToGroupDetails = {}
//        )
//    }
//}
//
//@Preview(showBackground = true, name = "Groups Screen Empty")
//@Composable
//fun GroupsScreenEmptyPreview() {
//    MaterialTheme {
//        GroupsScreen(
//            viewModel = MockGroupsViewModel(initialState = GroupsScreenUiState(userGroups = emptyList())),
//            onNavigateToGroupDetails = {}
//        )
//    }
//}

// A simple Mock ViewModel for Previews
//class MockGroupsViewModel(initialState: GroupsScreenUiState = GroupsScreenUiState(
//    userGroups = listOf(
//        GroupPreview("group1", "Weekend Warriors", 5, "Focusing on weekend projects", "John posted 2h ago"),
//        GroupPreview("group2", "Daily Coders", 12, "Coding every day challenge!", "Jane completed a task"),
//    )
//)) : GroupsViewModel(authService = object: AuthService { // Mock AuthService
//    override fun getCurrentUserId(): String? = "previewUser"
//    override fun isUserLoggedIn(): kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
//    override suspend fun signIn(email: String, password: String): Result<Unit> = Result.Success(Unit)
//    override suspend fun signUp(email: String, password: String): Result<String?> = Result.Success("previewUser")
//    override suspend fun signOut(): Result<Unit> = Result.Success(Unit)
//    override suspend fun getCurrentUserEmail(): String? = "preview@example.com"
//}, groupRepository = object : GroupRepository { // Mock GroupRepository
//    override suspend fun getGroupById(groupId: String): Result<GroupData?> = Result.Success(null)
//    override suspend fun getGroupsForUser(userId: String): Result<List<GroupData>> = Result.Success(emptyList())
//    override suspend fun insertGroup(group: GroupData): Result<String> = Result.Success("newGroupId")
//    override suspend fun updateGroup(group: GroupData): Result<Unit> = Result.Success(Unit)
//    override suspend fun removeUserFromGroup(groupId: String, userId: String): Result<Unit> = Result.Success(Unit)
//    override suspend fun addUserToGroup(groupId: String, userId: String): Result<Unit> = Result.Success(Unit)
//    override suspend fun findGroupByCode(groupCode: String): Result<GroupData?> = Result.Success(null)
//}) {
//    init {
//        _uiState.value = initialState
//    }
//    override fun loadUserGroups() {} // No-op for mock
//    override fun createGroup(name: String, description: String?) {} // No-op
//    override fun joinGroup(groupCodeToJoin: String) {} // No-op
//}