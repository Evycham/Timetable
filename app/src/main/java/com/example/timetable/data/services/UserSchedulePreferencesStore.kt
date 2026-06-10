package com.example.timetable.data.services

import com.example.timetable.data.datenmodell.HiddenLessonRule
import com.example.timetable.data.datenmodell.LessonSelection
import com.example.timetable.data.datenmodell.UserSchedulePreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class UserSchedulePreferencesStore(
    private val storageDir: File
) {

    fun load(): UserSchedulePreferences {
        val file = storageDir.resolve(PREFERENCES_FILE_NAME)
        if (!file.exists()) return UserSchedulePreferences()

        val root = JSONObject(file.readText())
        val groupsCode = root.optString("groupsCode").takeIf { it.isNotBlank() }

        val extraLessons = buildSet {
            val array = root.optJSONArray("extraLessons") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val lessonId = item.optString("lessonId").trim().ifEmpty { null }
                val code = item.optString("groupsCode").trim().ifEmpty { null }
                val title = item.optString("title").trim().ifEmpty { null }
                if (lessonId != null || (code != null && title != null)) {
                    add(LessonSelection(lessonId = lessonId, groupsCode = code, title = title))
                }
            }
        }

        val hiddenLessons = buildSet {
            val array = root.optJSONArray("hiddenLessons") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val lessonId = item.optString("lessonId").trim().ifEmpty { null }
                val title = item.optString("title").trim().ifEmpty { null }
                val code = item.optString("groupsCode").trim().ifEmpty { null }
                if (lessonId != null || title != null) {
                    add(HiddenLessonRule(lessonId = lessonId, title = title, groupsCode = code))
                }
            }
        }

        return UserSchedulePreferences(
            groupsCode = groupsCode,
            extraLessons = extraLessons,
            hiddenLessons = hiddenLessons
        )
    }

    fun save(preferences: UserSchedulePreferences) {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val root = JSONObject().apply {
            put("groupsCode", preferences.groupsCode ?: "")
            put(
                "extraLessons",
                JSONArray().apply {
                    preferences.extraLessons.forEach { lesson ->
                        put(
                            JSONObject().apply {
                                put("lessonId", lesson.lessonId ?: "")
                                put("groupsCode", lesson.groupsCode ?: "")
                                put("title", lesson.title ?: "")
                            }
                        )
                    }
                }
            )
            put(
                "hiddenLessons",
                JSONArray().apply {
                    preferences.hiddenLessons.forEach { rule ->
                        put(
                            JSONObject().apply {
                                put("lessonId", rule.lessonId ?: "")
                                put("title", rule.title ?: "")
                                put("groupsCode", rule.groupsCode ?: "")
                            }
                        )
                    }
                }
            )
        }

        storageDir.resolve(PREFERENCES_FILE_NAME).writeText(root.toString())
    }

    fun clear() {
        val file = storageDir.resolve(PREFERENCES_FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }

    companion object {
        const val PREFERENCES_FILE_NAME = "user_schedule_preferences.json"
    }
}
