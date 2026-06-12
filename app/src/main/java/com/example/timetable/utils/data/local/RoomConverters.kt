package com.example.timetable.utils.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

class RoomConverters {

    @TypeConverter
    fun fromStringSet(value: Set<String>?): String {
        if (value.isNullOrEmpty()) return "[]"
        return JSONArray(value.sorted()).toString()
    }

    @TypeConverter
    fun toStringSet(value: String?): Set<String>? {
        if (value.isNullOrBlank()) return null

        val array = JSONArray(value)
        val result = linkedSetOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optString(i).trim()
            if (item.isNotEmpty()) {
                result += item
            }
        }
        return result.takeIf { it.isNotEmpty() }
    }
}
