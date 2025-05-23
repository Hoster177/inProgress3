package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import ru.hoster.inprogress.data.Converters // Reusing Converters
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Entity(tableName = "groups")
@TypeConverters(Converters::class)
data class Group(
    @PrimaryKey // Firestore document ID will be the primary key for groups
    var firebaseId: String = "", // Group ID from Firestore

    var name: String,
    var description: String?,
    var joinCode: String, // Unique code to join this group
    var creatorUid: String, // UID of the user who created the group

    var membersUids: List<String> = listOf(), // List of member UIDs

    @ServerTimestamp
    var createdAt: Date? = null
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", null, "", "", listOf(), null)
}