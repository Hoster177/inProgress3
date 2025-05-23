package ru.hoster.inprogress.data

import androidx.room.TypeConverter
import ru.hoster.inprogress.data.GoalType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromGoalType(value: String?): GoalType? {
        return value?.let { GoalType.valueOf(it) }
    }

    @TypeConverter
    fun goalTypeToString(goalType: GoalType?): String? {
        return goalType?.name
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null) {
            return null
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        if (list == null) {
            return null
        }
        return Gson().toJson(list)
    }
}