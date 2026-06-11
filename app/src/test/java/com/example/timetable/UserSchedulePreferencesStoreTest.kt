package com.example.timetable

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.timetable.data.UserSchedulePreferencesStore
import com.example.timetable.data.datenmodell.HiddenLessonRule
import com.example.timetable.data.datenmodell.LessonSelection
import com.example.timetable.data.datenmodell.UserSchedulePreferences
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
        val store = UserSchedulePreferencesStore(
            PreferenceDataStoreFactory.create(
                produceFile = { tempDir.resolve("user-preferences.preferences_pb") }
            )
        )

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
}
