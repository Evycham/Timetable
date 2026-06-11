package com.example.timetable

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.timetable.data.TimetableRepository
import com.example.timetable.data.UserSchedulePreferencesStore
import com.example.timetable.data.local.TimetableDatabase
import com.example.timetable.data.services.DaVinciApi
import com.example.timetable.data.services.UserTimetableService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
class UserTimetableServiceTest {

    @Test
    fun buildUserLessons_usesGroupsCode_extraLessons_andHiddenRules() = runBlocking {
        val tempDir = Files.createTempDirectory("user-timetable-service-test").toFile()
        val database = createDatabase()
        val repository = TimetableRepository(
            context = applicationContext(),
            api = DaVinciApi(downloader = { sampleJson() }),
            database = database
        )
        repository.initialize()

        val preferencesStore = createPreferencesStore(tempDir)
        val userService = UserTimetableService(repository, preferencesStore)
        val matheLessons = repository.getLessonsByTitleAndGroupsCode("Mathe", "mb-MBB_4").sortedBy { it.date }
        val informatikLesson = repository.getLessonsByTitleAndGroupsCode("Informatik", "eti-SKIB_4").first()

        userService.setGroupsCode("mb-MBB_4")
        userService.setSetupComplete(true)
        userService.addExtraLessonById(informatikLesson.id)
        userService.hideLessonById(matheLessons.first().id)

        val userLessons = userService.buildUserLessons()
        val userDays = userService.buildUserCalenderDays()
        val preferences = userService.getPreferences()

        assertTrue(preferences.isSetupComplete)
        assertEquals(2, repository.getLessonsByTitleAndGroupsCode("Mathe", "mb-MBB_4").size)
        assertEquals(2, userLessons.size)
        assertEquals(listOf("Mathe", "Informatik"), userLessons.map { it.title })
        assertEquals(listOf("2026-05-19", "2026-05-20"), userDays.map { it.date })
        assertTrue(userDays.any { day -> day.events.isNotEmpty() })

        database.close()
    }

    private fun createPreferencesStore(tempDir: java.io.File): UserSchedulePreferencesStore {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempDir.resolve("user-timetable.preferences_pb") }
        )
        return UserSchedulePreferencesStore(dataStore)
    }

    private fun createDatabase(): TimetableDatabase =
        Room.inMemoryDatabaseBuilder(
            applicationContext(),
            TimetableDatabase::class.java
        ).allowMainThreadQueries().build()

    private fun applicationContext(): Context = ApplicationProvider.getApplicationContext()

    private fun sampleJson(): String = """
        {
          "result": {
            "displaySchedule": {
              "lessonTimes": [
                {
                  "courseTitle": "Mathe",
                  "dates": ["20260518", "20260519"],
                  "startTime": "0945",
                  "endTime": "1115",
                  "classCodes": ["mb-MBB_4"]
                },
                {
                  "courseTitle": "Informatik",
                  "dates": ["20260520"],
                  "startTime": "1200",
                  "endTime": "1330",
                  "classCodes": ["eti-SKIB_4"]
                }
              ],
              "eventTimes": [
                {
                  "id": "event-1",
                  "title": "Feiertag",
                  "startDate": "20260520",
                  "endDate": "20260520",
                  "category": "Feiertag"
                }
              ]
            }
          }
        }
    """.trimIndent()
}
