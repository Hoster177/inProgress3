package ru.hoster.inprogress.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserData(
    val userId: String = "",
    val displayName: String = "Unknown User",
    val email: String? = null,
    val avatarUrl: String? = null,
    @ServerTimestamp val createdAt: Date? = null // Firestore заполнит это
) {
    constructor() : this("", "Unknown User", null, null, null)
}