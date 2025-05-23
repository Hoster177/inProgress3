package ru.hoster.inprogress.domain.model // ИСПРАВЛЕННЫЙ ПАКЕТ

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class GroupData(
    val id: String = "",
    val name: String = "Unnamed Group",
    val description: String? = null,
    val adminUserId: String = "",
    val memberUserIds: List<String> = emptyList(),
    val groupCode: String? = null,
    @ServerTimestamp val createdAt: Date? = null // Firestore заполнит это
) {
    // Добавьте конструктор без аргументов для десериализации Firestore
    constructor() : this("", "Unnamed Group", null, "", emptyList(), null, null)
}