package ru.hoster.inprogress.navigation.groups // Or your preferred package for group feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.hoster.inprogress.domain.model.AuthService
import ru.hoster.inprogress.domain.model.GroupData
import ru.hoster.inprogress.domain.model.GroupRepository
import ru.hoster.inprogress.domain.model.Result
import java.security.SecureRandom
import javax.inject.Inject

// Re-using GroupPreview and GroupsScreenUiState from your GroupsScreen.kt stub
// If they are in a different file, ensure correct import.
// For clarity, I'll define them here if they are meant to be part of the ViewModel's contract.

data class GroupPreview( // Assuming this is defined in GroupsScreen.kt or a shared model file
    val id: String,
    val name: String,
    val memberCount: Int,
    val description: String? = null,
    val lastActivity: String? = "Нет недавней активности" // Placeholder
)

data class GroupsScreenUiState( // Assuming this is defined in GroupsScreen.kt or a shared model file
    val userGroups: List<GroupPreview> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val joinGroupDialogVisible: Boolean = false,
    val createGroupDialogVisible: Boolean = false,
    val isProcessingAction: Boolean = false, // For create/join progress
    val actionSuccessMessage: String? = null
)

// Helper to generate group codes (can be moved to a utility file)
fun generateRandomGroupCode(length: Int = 6): String {
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val random = SecureRandom()
    return (1..length)
        .map { characters[random.nextInt(characters.length)] }
        .joinToString("")
}

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val authService: AuthService,
    private val groupRepository: GroupRepository // Assuming FirestoreGroupRepository implements this
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsScreenUiState())
    val uiState: StateFlow<GroupsScreenUiState> = _uiState.asStateFlow()

    init {
        loadUserGroups()
    }

    fun loadUserGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = authService.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Пользователь не авторизован.") }
                return@launch
            }

            when (val result = groupRepository.getGroupsForUser(userId)) {
                is Result.Success -> {
                    val groupPreviews = result.data.map { groupData ->
                        groupData.toGroupPreview()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userGroups = groupPreviews.sortedBy { gp -> gp.name }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Не удалось загрузить группы: ${result.message.localizedMessage}"
                        )
                    }
                }
            }
        }
    }

    fun createGroup(name: String, description: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingAction = true, error = null, actionSuccessMessage = null) }
            val userId = authService.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isProcessingAction = false, error = "Не удалось создать группу: Пользователь не авторизован.") }
                return@launch
            }

            if (name.isBlank()) {
                _uiState.update { it.copy(isProcessingAction = false, error = "Название группы не может быть пустым.") }
                return@launch
            }

            // TODO: Check for group code uniqueness in a loop if necessary, or ensure DB handles it.
            // For simplicity, we generate one and hope for the best. A real app needs robust unique code generation.
            val groupCode = generateRandomGroupCode()

            val newGroup = GroupData(
                name = name,
                description = description?.takeIf { it.isNotBlank() },
                adminUserId = userId,
                memberUserIds = listOf(userId), // Creator is the first member and admin
                groupCode = groupCode
                // id and createdAt will be set by repository/Firestore
            )

            when (val result = groupRepository.insertGroup(newGroup)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessingAction = false,
                            createGroupDialogVisible = false,
                            actionSuccessMessage = "Группа \"${newGroup.name}\" успешно создана!"
                        )
                    }
                    loadUserGroups() // Refresh the list
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingAction = false,
                            error = "Не удалось создать группу: ${result.message.localizedMessage}"
                        )
                    }
                }
            }
        }
    }

    fun joinGroup(groupCodeToJoin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingAction = true, error = null, actionSuccessMessage = null) }
            val userId = authService.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isProcessingAction = false, error = "Не удалось присоединиться: Пользователь не авторизован.") }
                return@launch
            }
            if (groupCodeToJoin.isBlank()) {
                _uiState.update { it.copy(isProcessingAction = false, error = "Код группы не может быть пустым.") }
                return@launch
            }

            // Assume findGroupByCode is added to GroupRepository and its implementation
            // For now, I'll simulate this part or assume it exists in your GroupRepository.
            // You'll need to add: `suspend fun findGroupByCode(groupCode: String): Result<GroupData?>`
            // to your GroupRepository interface and implement it in FirestoreGroupRepository.

            // Placeholder for findGroupByCode logic:
            // Let's assume you've added findGroupByCode to your GroupRepository
            val findResult = (groupRepository as? ru.hoster.inprogress.data.repository.FirestoreGroupRepository)?.findGroupByCode(groupCodeToJoin)
                ?: run {
                    _uiState.update { it.copy(isProcessingAction = false, error = "Функция поиска группы по коду не реализована в репозитории.")}
                    return@launch
                }


            when (findResult) {
                is Result.Success -> {
                    val groupToJoin = findResult.data
                    if (groupToJoin == null) {
                        _uiState.update { it.copy(isProcessingAction = false, error = "Группа с кодом \"$groupCodeToJoin\" не найдена.") }
                        return@launch
                    }
                    if (groupToJoin.memberUserIds.contains(userId)) {
                        _uiState.update { it.copy(isProcessingAction = false, error = "Вы уже являетесь участником этой группы.") }
                        return@launch
                    }

                    when (val addResult = groupRepository.addUserToGroup(groupToJoin.id, userId)) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    isProcessingAction = false,
                                    joinGroupDialogVisible = false,
                                    actionSuccessMessage = "Вы успешно присоединились к группе \"${groupToJoin.name}\"!"
                                )
                            }
                            loadUserGroups() // Refresh the list
                        }
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isProcessingAction = false,
                                    error = "Не удалось присоединиться к группе: ${addResult.message.localizedMessage}"
                                )
                            }
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingAction = false,
                            error = "Ошибка при поиске группы: ${findResult.message.localizedMessage}"
                        )
                    }
                }
            }
        }
    }

    fun showJoinGroupDialog(show: Boolean) {
        _uiState.update { it.copy(joinGroupDialogVisible = show, error = null, actionSuccessMessage = null) }
    }

    fun showCreateGroupDialog(show: Boolean) {
        _uiState.update { it.copy(createGroupDialogVisible = show, error = null, actionSuccessMessage = null) }
    }

    fun clearActionSuccessMessage() {
        _uiState.update { it.copy(actionSuccessMessage = null) }
    }
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun GroupData.toGroupPreview(): GroupPreview {
        return GroupPreview(
            id = this.id,
            name = this.name,
            memberCount = this.memberUserIds.size,
            description = this.description,
            lastActivity = "Нет недавней активности" // Placeholder, needs real data source
        )
    }
}