package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date


@Entity(
    tableName = "activity_items",
    indices = [Index(value = ["firebaseId"], unique = true)] // firebaseId должен быть уникальным, если он есть
)
data class ActivityItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L, // Локальный автоинкрементный ID
    val firebaseId: String? = null, // ID из Firebase, может быть null для локально созданных
    val userId: String, // ID пользователя, которому принадлежит задача
    val name: String,
    var totalDurationMillisToday: Long = 0L,
    var isActive: Boolean = false,
    val colorHex: String? = null, // Assuming it's a nullable String
    val createdAt: Date = Date() // Дата создания или дата, к которой относится активность
    // Добавь другие поля, если они есть, например lastUpdatedAt: Date? = null
)