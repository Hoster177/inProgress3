package ru.hoster.inprogress.data.fake

import ru.hoster.inprogress.domain.model.GroupData
import ru.hoster.inprogress.domain.model.GroupRepository
import ru.hoster.inprogress.domain.model.Result
import kotlinx.coroutines.delay
import java.util.Date
import java.util.UUID

class FakeGroupRepository : GroupRepository {

    private val groups = mutableMapOf<String, GroupData>()

    init {
        // Pre-populate with some fake groups
        val group1Id = "group_id_1"
        val group2Id = "group_id_2"
        val adminUser = "user_admin_123"
        val currentUser = "Hoster177_fake_id"

        addGroup(
            GroupData(
                id = group1Id,
                name = "Weekend Warriors (Fake)",
                description = "Focusing on weekend projects (Fake Data).",
                adminUserId = adminUser,
                memberUserIds = listOf(adminUser, currentUser, "user_member_3"),
                groupCode = "WKND-FAKE",
                createdAt = Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 5) // 5 days ago
            )
        )
        addGroup(
            GroupData(
                id = group2Id,
                name = "Daily Coders (Fake)",
                description = "Coding every day challenge! (Fake Data)",
                adminUserId = currentUser,
                memberUserIds = listOf(currentUser, "user_member_4"),
                groupCode = "DAILY-FAKE",
                createdAt = Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2) // 2 days ago
            )
        )
        addGroup(
            GroupData( // Group for testing leaving when admin is only member
                id = "group_admin_only_id",
                name = "Admin Only Group",
                description = "A group with only an admin.",
                adminUserId = adminUser,
                memberUserIds = listOf(adminUser),
                groupCode = "ADMIN-ONLY",
                createdAt = Date()
            )
        )
    }

    fun addGroup(group: GroupData) {
        groups[group.id] = group
    }

    override suspend fun getGroupById(groupId: String): Result<GroupData?> {
        delay(500) // Simulate network delay
        val group = groups[groupId]
        return if (group != null) {
            Result.Success(group)
        } else {
            // Result.Success(null) // To indicate group not found but no system error
            Result.Error(Exception("FakeRepo: Group with ID $groupId not found.")) // Or an error
        }
    }

    override suspend fun getGroupsForUser(userId: String): Result<List<GroupData>> {
        TODO("Not yet getGroupsForUser")
    }

    override suspend fun insertGroup(group: GroupData): Result<String> {
        TODO("Not yet getGroupsForUser")
    }

    override suspend fun updateGroup(group: GroupData): Result<Unit> {
        TODO("Not yet getGroupsForUser")
    }

    override suspend fun removeUserFromGroup(groupId: String, userId: String): Result<Unit> {
        delay(700) // Simulate network delay
        val group = groups[groupId]
        return if (group == null) {
            Result.Error(Exception("FakeRepo: Group with ID $groupId not found for removal."))
        } else {
            if (!group.memberUserIds.contains(userId)) {
                // Optionally treat as error or success if user wasn't a member
                // return Result.Error(Exception("FakeRepo: User $userId not in group $groupId."))
                println("FakeRepo: User $userId was not in group $groupId, no action taken.")
                Result.Success(Unit) // User wasn't there, so effectively removed
            } else {
                val updatedMembers = group.memberUserIds.filterNot { it == userId }
                // Handle admin leaving scenario (simplified: if admin leaves, make someone else admin or delete group)
                // For this fake, we'll just remove. A real implementation needs more robust logic.
                if (userId == group.adminUserId && updatedMembers.isEmpty()) {
                    // This scenario is tricky. The ViewModel has a check, but repo could also enforce rules.
                    // For simplicity, let's allow removal here and let ViewModel handle UI message.
                    // OR: return Result.Error(Exception("Admin cannot be removed if it leaves group empty/adminless without transfer"))
                    println("FakeRepo: Admin $userId removed, leaving group $groupId potentially adminless or empty.")
                }
                groups[groupId] = group.copy(memberUserIds = updatedMembers)
                Result.Success(Unit)
            }
        }
    }

    override suspend fun addUserToGroup(groupId: String, userId: String): Result<Unit> {
        TODO("Not yet addUserToGroup")
    }

    override suspend fun findGroupByCode(groupCode: String): Result<GroupData?> {
        TODO("Not yet implemented")
    }

    // --- Helper methods for testing ---
    fun clearGroups() {
        groups.clear()
    }

    fun getGroupDirectly(groupId: String): GroupData? {
        return groups[groupId]
    }
}
