package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import ru.hoster.inprogress.data.Converters
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Entity(tableName = "groups")
@TypeConverters(Converters::class)
data class Group(
    @PrimaryKey
    var firebaseId: String = "",

    var name: String,
    var description: String?,
    var joinCode: String,
    var creatorUid: String,

    var membersUids: List<String> = listOf(),

    @ServerTimestamp
    var createdAt: Date? = null
) {
    constructor() : this("", "", null, "", "", listOf(), null)
}