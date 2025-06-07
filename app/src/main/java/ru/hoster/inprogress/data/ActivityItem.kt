package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date


@Entity(
    tableName = "activity_items",
    indices = [Index(value = ["firebaseId"], unique = true)]
)
data class ActivityItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val firebaseId: String? = null,
    val userId: String,
    val name: String,
    var totalDurationMillisToday: Long = 0L,
    var isActive: Boolean = false,
    val colorHex: String? = null,
    @ServerTimestamp
    val createdAt: Date = Date()
)