package ru.hoster.inprogress.navigation.groups// Убедитесь, что пакет правильный

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.hoster.inprogress.domain.model.AuthService
import ru.hoster.inprogress.domain.model.GroupRepository
import ru.hoster.inprogress.domain.model.UserRepository
import ru.hoster.inprogress.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI-специфичные модели и Navigation Signal ---
data class GroupDetailDisplay(
    val id: String,
    val name: String,
    val description: String?,
    val adminUserId: String,
    val groupCode: String?
)

data class MemberDisplay(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val isAdmin: Boolean
)

data class GroupDetailsScreenUiState(
    val group: GroupDetailDisplay? = null,
    val members: List<MemberDisplay> = emptyList(),
    val isLoading: Boolean = false,
    val isCurrentUserAdmin: Boolean = false,
    val currentUserId: String? = null,
    val error: String? = null,
    val showLeaveGroupDialog: Boolean = false,
    val leaveGroupInProgress: Boolean = false,
    val navigationSignal: GroupDetailsNavigationSignal? = null
)

sealed class GroupDetailsNavigationSignal {
    object NavigateBack : GroupDetailsNavigationSignal()
    data class NavigateToEditGroup(val groupId: String) : GroupDetailsNavigationSignal()
}
// --- Конец UI-специфичных моделей ---

@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val authService: AuthService
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId")!!

    private val _uiState = MutableStateFlow(GroupDetailsScreenUiState(isLoading = true))
    val uiState: StateFlow<GroupDetailsScreenUiState> = _uiState.asStateFlow()

    init {
        val fetchedCurrentUserId = authService.getCurrentUserId()
        _uiState.update { it.copy(currentUserId = fetchedCurrentUserId) }

        if (groupId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Group ID is missing.") }
        } else {
            loadGroupDetails()
        }
    }

    fun loadGroupDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentUserIdFromState = _uiState.value.currentUserId
            if (currentUserIdFromState == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not authenticated (from state).") }
                return@launch
            }

            when (val groupResult = groupRepository.getGroupById(groupId)) {
                is Result.Success -> {
                    val groupData = groupResult.data
                    if (groupData == null) {
                        _uiState.update { it.copy(isLoading = false, error = "Group not found.") }
                        return@launch
                    }

                    val groupDisplay = groupData.toGroupDetailDisplay()
                    val isAdmin = groupData.adminUserId == currentUserIdFromState

                    if (groupData.memberUserIds.isNotEmpty()) {
                        val memberIdsToFetch = if (groupData.memberUserIds.size > 30) {
                            println("Warning: Fetching only first 30 members out of ${groupData.memberUserIds.size}")
                            groupData.memberUserIds.take(30)
                        } else {
                            groupData.memberUserIds
                        }

                        if (memberIdsToFetch.isEmpty() && groupData.memberUserIds.isNotEmpty()){
                            _uiState.update {
                                it.copy(
                                    group = groupDisplay,
                                    members = emptyList(),
                                    isCurrentUserAdmin = isAdmin,
                                    isLoading = false,
                                    error = if(groupData.memberUserIds.size > 30) "Too many members to fetch all details at once." else null
                                )
                            }
                            return@launch
                        }


                        when (val membersResult = userRepository.getUsersByIds(memberIdsToFetch)) {
                            is Result.Success -> {
                                val memberDisplays = membersResult.data.map { userData ->
                                    userData.toMemberDisplay(isAdmin = userData.userId == groupData.adminUserId)
                                }
                                _uiState.update {
                                    it.copy(
                                        group = groupDisplay,
                                        members = memberDisplays,
                                        isCurrentUserAdmin = isAdmin,
                                        isLoading = false
                                    )
                                }
                            }
                            is Result.Error -> {
                                _uiState.update {
                                    it.copy(
                                        group = groupDisplay,
                                        isCurrentUserAdmin = isAdmin,
                                        isLoading = false,
                                        // Corrected: Use .message.message
                                        error = "Failed to load members: ${membersResult.message.message}"
                                    )
                                }
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                group = groupDisplay,
                                members = emptyList(),
                                isCurrentUserAdmin = isAdmin,
                                isLoading = false
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        // Corrected: Use .message.message
                        it.copy(isLoading = false, error = "Failed to load group: ${groupResult.message.message}")
                    }
                }
            }
        }
    }

    fun confirmLeaveGroup(show: Boolean) {
        _uiState.update { it.copy(showLeaveGroupDialog = show) }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            val currentUserIdFromState = _uiState.value.currentUserId
            if (currentUserIdFromState == null) {
                _uiState.update { it.copy(error = "Cannot leave group: User not authenticated.", showLeaveGroupDialog = false) }
                return@launch
            }
            if (_uiState.value.group == null) {
                _uiState.update { it.copy(error = "Cannot leave group: Group data not loaded.", showLeaveGroupDialog = false) }
                return@launch
            }

            if (_uiState.value.isCurrentUserAdmin && _uiState.value.members.size <= 1) {
                _uiState.update { it.copy(error = "Admin cannot leave if they are the only member. Delete or transfer admin first.", showLeaveGroupDialog = false) }
                return@launch
            }

            _uiState.update { it.copy(leaveGroupInProgress = true, showLeaveGroupDialog = false) }
            when (val result = groupRepository.removeUserFromGroup(groupId, currentUserIdFromState)) {
                is Result.Success -> {
                    _uiState.update { it.copy(leaveGroupInProgress = false, navigationSignal = GroupDetailsNavigationSignal.NavigateBack) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            leaveGroupInProgress = false,
                            // Corrected: Use .message.message
                            error = "Failed to leave group: ${result.message.message}"
                        )
                    }
                }
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigationSignal = null) }
    }

    fun navigateToEditGroup() {
        _uiState.update { it.copy(navigationSignal = GroupDetailsNavigationSignal.NavigateToEditGroup(groupId)) }
    }

    // --- Mapper functions ---
    private fun ru.hoster.inprogress.domain.model.GroupData.toGroupDetailDisplay(): GroupDetailDisplay {
        return GroupDetailDisplay(
            id = this.id,
            name = this.name,
            description = this.description,
            adminUserId = this.adminUserId,
            groupCode = this.groupCode
        )
    }

    private fun ru.hoster.inprogress.domain.model.UserData.toMemberDisplay(isAdmin: Boolean): MemberDisplay {
        return MemberDisplay(
            userId = this.userId,
            displayName = this.displayName,
            avatarUrl = this.avatarUrl,
            isAdmin = isAdmin
        )
    }
}