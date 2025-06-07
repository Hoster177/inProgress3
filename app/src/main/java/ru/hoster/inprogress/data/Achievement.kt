package ru.hoster.inprogress.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements_definitions")
data class Achievement(
    @PrimaryKey
    var id: String,

    var name: String,
    var description: String,
    var iconRef: String?,
    var xpReward: Long = 0L
) {

    constructor() : this("", "", "", null, 0L)
}