package ru.hoster.inprogress.domain.model

interface GroupRepository {
    suspend fun getGroupById(groupId: String): Result<GroupData?>
    suspend fun getGroupsForUser(userId: String): Result<List<GroupData>> // Новый полезный метод
    suspend fun insertGroup(group: GroupData): Result<String> // Возвращает ID новой группы
    suspend fun updateGroup(group: GroupData): Result<Unit>
    suspend fun removeUserFromGroup(groupId: String, userId: String): Result<Unit>
    suspend fun addUserToGroup(groupId: String, userId: String): Result<Unit> // Новый полезный метод
    suspend fun findGroupByCode(groupCode: String): Result<GroupData?>
}