package com.example.timetable.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.timetable.data.datenmodell.HiddenLessonRule
import com.example.timetable.data.datenmodell.LessonSelection
import com.example.timetable.data.datenmodell.UserSchedulePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

val Context.userSchedulePreferencesDataStore by preferencesDataStore(
    name = UserSchedulePreferencesStore.DATASTORE_NAME
)

class UserSchedulePreferencesStore(
    private val dataStore: DataStore<Preferences>
) {

    val preferencesFlow: Flow<UserSchedulePreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map(::mapPreferences)

    suspend fun load(): UserSchedulePreferences = preferencesFlow.first()

    suspend fun save(preferences: UserSchedulePreferences) {
        dataStore.edit { store ->
            writePreferences(store, preferences)
        }
    }

    suspend fun update(transform: (UserSchedulePreferences) -> UserSchedulePreferences) {
        dataStore.edit { store ->
            val current = mapPreferences(store)
            val updated = transform(current)
            writePreferences(store, updated)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun mapPreferences(preferences: Preferences): UserSchedulePreferences {
        val groupsCode = preferences[GROUPS_CODE_KEY]
        val isSetupComplete = preferences[IS_SETUP_COMPLETE_KEY] ?: false
        val extraLessons = decodeExtraLessons(preferences[EXTRA_LESSONS_KEY].orEmpty())
        val hiddenLessons = decodeHiddenLessons(preferences[HIDDEN_LESSONS_KEY].orEmpty())

        return UserSchedulePreferences(
            isSetupComplete = isSetupComplete,
            groupsCode = groupsCode,
            extraLessons = extraLessons,
            hiddenLessons = hiddenLessons
        )
    }

    private fun writePreferences(
        store: androidx.datastore.preferences.core.MutablePreferences,
        preferences: UserSchedulePreferences
    ) {
        store[IS_SETUP_COMPLETE_KEY] = preferences.isSetupComplete

        if (preferences.groupsCode.isNullOrBlank()) {
            store.remove(GROUPS_CODE_KEY)
        } else {
            store[GROUPS_CODE_KEY] = preferences.groupsCode
        }

        if (preferences.extraLessons.isEmpty()) {
            store.remove(EXTRA_LESSONS_KEY)
        } else {
            store[EXTRA_LESSONS_KEY] = encodeExtraLessons(preferences.extraLessons)
        }

        if (preferences.hiddenLessons.isEmpty()) {
            store.remove(HIDDEN_LESSONS_KEY)
        } else {
            store[HIDDEN_LESSONS_KEY] = encodeHiddenLessons(preferences.hiddenLessons)
        }
    }

    private fun encodeExtraLessons(extraLessons: Set<LessonSelection>): String =
        JSONArray().apply {
            extraLessons.forEach { lesson ->
                put(
                    JSONObject().apply {
                        put("lessonId", lesson.lessonId ?: "")
                        put("groupsCode", lesson.groupsCode ?: "")
                        put("title", lesson.title ?: "")
                    }
                )
            }
        }.toString()

    private fun decodeExtraLessons(raw: String): Set<LessonSelection> {
        if (raw.isBlank()) return emptySet()

        val array = JSONArray(raw)
        return buildSet {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val lessonId = item.optString("lessonId").trim().ifEmpty { null }
                val groupsCode = item.optString("groupsCode").trim().ifEmpty { null }
                val title = item.optString("title").trim().ifEmpty { null }
                if (lessonId != null || (groupsCode != null && title != null)) {
                    add(
                        LessonSelection(
                            lessonId = lessonId,
                            groupsCode = groupsCode,
                            title = title
                        )
                    )
                }
            }
        }
    }

    private fun encodeHiddenLessons(hiddenLessons: Set<HiddenLessonRule>): String =
        JSONArray().apply {
            hiddenLessons.forEach { rule ->
                put(
                    JSONObject().apply {
                        put("lessonId", rule.lessonId ?: "")
                        put("title", rule.title ?: "")
                        put("groupsCode", rule.groupsCode ?: "")
                    }
                )
            }
        }.toString()

    private fun decodeHiddenLessons(raw: String): Set<HiddenLessonRule> {
        if (raw.isBlank()) return emptySet()

        val array = JSONArray(raw)
        return buildSet {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val lessonId = item.optString("lessonId").trim().ifEmpty { null }
                val title = item.optString("title").trim().ifEmpty { null }
                val groupsCode = item.optString("groupsCode").trim().ifEmpty { null }
                if (lessonId != null || title != null) {
                    add(
                        HiddenLessonRule(
                            lessonId = lessonId,
                            title = title,
                            groupsCode = groupsCode
                        )
                    )
                }
            }
        }
    }

    companion object {
        const val DATASTORE_NAME = "user_schedule_preferences"

        private val IS_SETUP_COMPLETE_KEY = booleanPreferencesKey("is_setup_complete")
        private val GROUPS_CODE_KEY = stringPreferencesKey("groups_code")
        private val EXTRA_LESSONS_KEY = stringPreferencesKey("extra_lessons_json")
        private val HIDDEN_LESSONS_KEY = stringPreferencesKey("hidden_lessons_json")
    }
}
