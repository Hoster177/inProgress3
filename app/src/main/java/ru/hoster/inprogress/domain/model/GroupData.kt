package ru.hoster.inprogress.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class GroupData(
    val id: String = "",
    val name: String = "Unnamed Group",
    val description: String? = null,
    val adminUserId: String = "",
    val memberUserIds: List<String> = emptyList(),
    val groupCode: String? = null,
    @ServerTimestamp val createdAt: Date? = null
) {
    constructor() : this("", "Unnamed Group", null, "", emptyList(), null, null)
}