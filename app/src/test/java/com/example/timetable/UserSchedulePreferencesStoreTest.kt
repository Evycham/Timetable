package com.example.timetable

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.timetable.data.local.preferences.UserSchedulePreferences
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class UserSchedulePreferencesStoreTest {

    @Test
    fun save_and_load_preferences() = runBlocking {
        val tempDir = Files.createTempDirectory("preferences-store-test").toFile()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempDir.resolve("user-preferences.preferences_pb") }
        )
        val store = UserSchedulePreferencesStore(dataStore)

        val initialPrefs = store.preferencesFlow.first()
        assertEquals(null, initialPrefs.groupsCode)
        assertEquals("Mittel", initialPrefs.appFontSize)

        val newPrefs = UserSchedulePreferences(
            isSetupComplete = true,
            groupsCode = "mb-MBB_4",
            isDynamicColorEnabled = true,
            appFontSize = "Groß"
        )
        store.save(newPrefs)

        val preferences = store.preferencesFlow.first()
        assertTrue(preferences.isSetupComplete)
        assertEquals("mb-MBB_4", preferences.groupsCode)
        assertTrue(preferences.isDynamicColorEnabled)
        assertEquals("Groß", preferences.appFontSize)
    }

    @Test
    fun save_and_load_preferences_with_emojis() = runBlocking {
        val tempDir = Files.createTempDirectory("preferences-store-emojis-test").toFile()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempDir.resolve("user-preferences.preferences_pb") }
        )
        val store = UserSchedulePreferencesStore(dataStore)

        val emojis = mapOf("Maths" to "📐", "Physics" to "⚛️")
        val newPrefs = UserSchedulePreferences(
            isSetupComplete = true,
            groupsCode = "mb-MBB_4",
            isDynamicColorEnabled = true,
            moduleEmojis = emojis
        )
        store.save(newPrefs)

        val preferences = store.preferencesFlow.first()
        assertEquals(emojis, preferences.moduleEmojis)
        assertEquals("📐", preferences.moduleEmojis["Maths"])
        assertEquals("⚛️", preferences.moduleEmojis["Physics"])

        // Update to empty
        store.update { it.copy(moduleEmojis = emptyMap()) }
        val updatedPrefs = store.preferencesFlow.first()
        assertTrue(updatedPrefs.moduleEmojis.isEmpty())
    }

    @Test
    fun save_and_load_preferences_with_font_size() = runBlocking {
        val tempDir = Files.createTempDirectory("preferences-store-font-test").toFile()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempDir.resolve("user-preferences.preferences_pb") }
        )
        val store = UserSchedulePreferencesStore(dataStore)

        val newPrefs = UserSchedulePreferences(
            isSetupComplete = true,
            groupsCode = "mb-MBB_4",
            appFontSize = "Groß"
        )
        store.save(newPrefs)

        val preferences = store.preferencesFlow.first()
        assertEquals("Groß", preferences.appFontSize)

        // Update to small
        store.update { it.copy(appFontSize = "Klein") }
        val updatedPrefs = store.preferencesFlow.first()
        assertEquals("Klein", updatedPrefs.appFontSize)
    }
}
