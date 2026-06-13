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

        val newPrefs = UserSchedulePreferences(
            isSetupComplete = true,
            groupsCode = "mb-MBB_4",
            isDynamicColorEnabled = true
        )
        store.save(newPrefs)

        val preferences = store.preferencesFlow.first()
        assertTrue(preferences.isSetupComplete)
        assertEquals("mb-MBB_4", preferences.groupsCode)
        assertTrue(preferences.isDynamicColorEnabled)
    }
}
