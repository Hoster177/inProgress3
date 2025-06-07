package ru.hoster.inprogress.domain.model

interface GroupRepository {
    suspend fun getGroupById(groupId: String): Result<GroupData?>
    suspend fun getGroupsForUser(userId: String): Result<List<GroupData>>
    suspend fun insertGroup(group: GroupData): Result<String>
    suspend fun updateGroup(group: GroupData): Result<Unit>
    suspend fun removeUserFromGroup(groupId: String, userId: String): Result<Unit>
    suspend fun addUserToGroup(groupId: String, userId: String): Result<Unit>
    suspend fun findGroupByCode(groupCode: String): Result<GroupData?>
}