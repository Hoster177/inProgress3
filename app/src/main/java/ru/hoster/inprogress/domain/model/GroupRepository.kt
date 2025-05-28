package ru.hoster.inprogress.domain.model // ИСПРАВЛЕННЫЙ ПАКЕТ

// Предполагаем, что Result уже определен (см. UserRepository.kt)

interface GroupRepository {
    suspend fun getGroupById(groupId: String): Result<GroupData?>
    suspend fun getGroupsForUser(userId: String): Result<List<GroupData>> // Новый полезный метод
    suspend fun insertGroup(group: GroupData): Result<String> // Возвращает ID новой группы
    suspend fun updateGroup(group: GroupData): Result<Unit>
    suspend fun removeUserFromGroup(groupId: String, userId: String): Result<Unit>
    suspend fun addUserToGroup(groupId: String, userId: String): Result<Unit> // Новый полезный метод
    suspend fun findGroupByCode(groupCode: String): Result<GroupData?>
    // Можно добавить другие методы:
    // suspend fun deleteGroup(groupId: String): Result<Unit>
    // suspend fun findGroupByCode(groupCode: String): Result<GroupData?>
}