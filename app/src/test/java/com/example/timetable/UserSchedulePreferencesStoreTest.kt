package com.example.timetable

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.timetable.utils.data.UserSchedulePreferencesStore
import com.example.timetable.utils.data.datenmodell.HiddenLessonRule
import com.example.timetable.utils.data.datenmodell.LessonSelection
import com.example.timetable.utils.data.datenmodell.UserSchedulePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class UserSchedulePreferencesStoreTest {

    @Test
    fun preferencesFlow_emitsStoredSetupState_andSelections() = runBlocking {
        val tempDir = Files.createTempDirectory("preferences-store-test").toFile()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempDir.resolve("user-preferences.preferences_pb") }
        )
        val store = UserSchedulePreferencesStore(dataStore)

        store.save(
            UserSchedulePreferences(
                isSetupComplete = true,
                groupsCode = "mb-MBB_4",
                extraLessons = setOf(LessonSelection(lessonId = "lesson-1")),
                hiddenLessons = setOf(HiddenLessonRule(title = "Mathe"))
            )
        )

        val preferences = store.preferencesFlow.first()

        assertTrue(preferences.isSetupComplete)
        assertEquals("mb-MBB_4", preferences.groupsCode)
        assertEquals(setOf(LessonSelection(lessonId = "lesson-1")), preferences.extraLessons)
        assertEquals(setOf(HiddenLessonRule(title = "Mathe")), preferences.hiddenLessons)
    }

    @Test
    fun preferencesFlow_returnsEmptyRules_whenStoredJsonIsCorrupted() = runBlocking {
        val tempDir = Files.createTempDirectory("preferences-store-corruption-test").toFile()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempDir.resolve("user-preferences.preferences_pb") }
        )
        val store = UserSchedulePreferencesStore(dataStore)

        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("extra_lessons_json")] = "{broken"
            preferences[stringPreferencesKey("hidden_lessons_json")] = "[broken"
        }

        val preferences = store.preferencesFlow.first()

        assertTrue(preferences.extraLessons.isEmpty())
        assertTrue(preferences.hiddenLessons.isEmpty())
    }
}
