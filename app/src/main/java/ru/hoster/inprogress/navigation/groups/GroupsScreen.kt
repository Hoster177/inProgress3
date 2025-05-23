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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class GroupPreview(
    val id: String,
    val name: String,
    val memberCount: Int,
    val description: String? = null,
    val lastActivity: String? = "No recent activity" // Formatted string
)

data class GroupsScreenUiState(
    val userGroups: List<GroupPreview> = emptyList(),
    val isLoading: Boolean = false,
    val joinGroupDialogVisible: Boolean = false,
    val createGroupDialogVisible: Boolean = false
)

// --- Conceptual ViewModel (GroupsViewModel.kt) ---
// class GroupsViewModel(
//    // private val groupRepository: YourGroupRepository,
//    // private val userRepository: YourUserRepository
// ) : ViewModel() {
//    private val _uiState = MutableStateFlow(GroupsScreenUiState())
//    val uiState: StateFlow<GroupsScreenUiState> = _uiState.asStateFlow()
//
//    init { loadUserGroups() }
//
//    fun loadUserGroups() { /* Fetch groups for current user */ }
//    fun createGroup(name: String, description: String?) { /* ... */ }
//    fun joinGroup(groupCode: String) { /* ... */ }
//    fun showJoinGroupDialog(show: Boolean) { _uiState.update { it.copy(joinGroupDialogVisible = show) } }
//    fun showCreateGroupDialog(show: Boolean) { _uiState.update { it.copy(createGroupDialogVisible = show) } }
// }
// --- End Conceptual ViewModel ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    // navController: NavController, // To navigate to individual group details
    // viewModel: GroupsViewModel = hiltViewModel() // Or your ViewModel provider
    // For preview, we'll use local state and mock actions
    onNavigateToGroupDetails: (groupId: String) -> Unit
) {
    // Simulate ViewModel state for preview
    var uiState by remember {
        mutableStateOf(
            GroupsScreenUiState(
                userGroups = listOf(
                    GroupPreview("group1", "Weekend Warriors", 5, "Focusing on weekend projects", "John posted 2h ago"),
                    GroupPreview("group2", "Daily Coders", 12, "Coding every day challenge!", "Jane completed a task"),
                    GroupPreview("group3", "Study Buddies", 3, "Preparing for exams", "Activity 5m ago")
                )
            )
        )
    }
    var groupCodeInput by remember { mutableStateOf("") }
    var newGroupNameInput by remember { mutableStateOf("") }
    var newGroupDescriptionInput by remember { mutableStateOf("") }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои Группы") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // No navigation icon needed if this is a top-level screen in bottom nav
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FloatingActionButton(
                    onClick = { uiState = uiState.copy(joinGroupDialogVisible = true) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Filled.GroupAdd, contentDescription = "Присоединиться к группе")
                }
                FloatingActionButton(
                    onClick = { uiState = uiState.copy(createGroupDialogVisible = true) }
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
            } else if (uiState.userGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Вы еще не состоите ни в одной группе. Создайте новую или присоединитесь к существующей!",
                        style = MaterialTheme.typography.bodyLarge,
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
                onDismissRequest = { uiState = uiState.copy(joinGroupDialogVisible = false); groupCodeInput = "" },
                title = { Text("Присоединиться к группе") },
                text = {
                    OutlinedTextField(
                        value = groupCodeInput,
                        onValueChange = { groupCodeInput = it },
                        label = { Text("Код группы") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        // TODO: Call viewModel.joinGroup(groupCodeInput)
                        println("Attempting to join group with code: $groupCodeInput")
                        uiState = uiState.copy(joinGroupDialogVisible = false)
                        groupCodeInput = ""
                    }) { Text("Присоединиться") }
                },
                dismissButton = {
                    TextButton(onClick = { uiState = uiState.copy(joinGroupDialogVisible = false); groupCodeInput = "" }) { Text("Отмена") }
                }
            )
        }

        // --- Dialog for Creating a Group ---
        if (uiState.createGroupDialogVisible) {
            AlertDialog(
                onDismissRequest = {
                    uiState = uiState.copy(createGroupDialogVisible = false)
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
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newGroupDescriptionInput,
                            onValueChange = { newGroupDescriptionInput = it },
                            label = { Text("Описание (необязательно)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        // TODO: Call viewModel.createGroup(newGroupNameInput, newGroupDescriptionInput)
                        println("Creating group: $newGroupNameInput, Desc: $newGroupDescriptionInput")
                        uiState = uiState.copy(createGroupDialogVisible = false)
                        newGroupNameInput = ""
                        newGroupDescriptionInput = ""
                    }) { Text("Создать") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        uiState = uiState.copy(createGroupDialogVisible = false)
                        newGroupNameInput = ""
                        newGroupDescriptionInput = ""
                    }) { Text("Отмена") }
                }
            )
        }
    }
}

@Composable
fun GroupItemCard(group: GroupPreview, onClick: () -> Unit) {
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
                if (group.lastActivity != null) {
                    Text(
                        "Активность: ${group.lastActivity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "Перейти к группе",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true, name = "Groups Screen with Data")
@Composable
fun GroupsScreenWithDataPreview() {
    MaterialTheme {
        GroupsScreen(onNavigateToGroupDetails = {})
    }
}

@Preview(showBackground = true, name = "Groups Screen Empty")
@Composable
fun GroupsScreenEmptyPreview() {
    MaterialTheme {
        // To preview empty state, we'd need to modify the state passed or the default in GroupsScreen
        // For simplicity, this preview will show the same as above unless state is directly manipulated here.
        // A better way for previews is to pass the uiState directly.
        val emptyState = GroupsScreenUiState(userGroups = emptyList())
        // This requires GroupsScreen to accept uiState as a parameter for preview purposes
        // For now, this preview will render the default state of the GroupsScreen.
        GroupsScreen(onNavigateToGroupDetails = {})
        // To actually show empty state, you'd do something like:
        // GroupsScreenComposable(uiState = GroupsScreenUiState(userGroups = emptyList()), onNavigate... = {})
        // where GroupsScreenComposable is the inner content part of GroupsScreen.
    }
}

@Preview(showBackground = true, name = "Join Group Dialog")
@Composable
fun JoinGroupDialogPreview() {
    MaterialTheme {
        // Simulate dialog being visible
        val uiState = GroupsScreenUiState(joinGroupDialogVisible = true)
        // This preview focuses on the dialog itself, not the full screen logic.
        // You'd typically preview dialogs in isolation.
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Присоединиться к группе") },
            text = {
                OutlinedTextField(
                    value = "ABC-123",
                    onValueChange = { },
                    label = { Text("Код группы") }
                )
            },
            confirmButton = { Button(onClick = {}) { Text("Присоединиться") } },
            dismissButton = { TextButton(onClick = {}) { Text("Отмена") } }
        )
    }
}