package com.example.timetable

import com.example.timetable.data.services.DaVinciApi
import com.example.timetable.data.services.TimetableRepository
import com.example.timetable.data.services.UserSchedulePreferencesStore
import com.example.timetable.data.services.UserTimetableService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class UserTimetableServiceTest {

    @Test
    fun buildUserLessons_usesGroupsCode_extraLessons_andHiddenRules() {
        val tempDir = Files.createTempDirectory("user-timetable-service-test").toFile()
        val repository = TimetableRepository(
            storageDir = tempDir,
            api = DaVinciApi(downloader = { sampleJson() })
        )
        repository.initialize()

        val preferencesStore = UserSchedulePreferencesStore(tempDir)
        val userService = UserTimetableService(repository, preferencesStore)
        val matheLessons = repository.getLessonsByTitleAndGroupsCode("Mathe", "mb-MBB_4").sortedBy { it.date }
        val informatikLesson = repository.getLessonsByTitleAndGroupsCode("Informatik", "eti-SKIB_4").first()

        userService.setGroupsCode("mb-MBB_4")
        userService.addExtraLessonById(informatikLesson.id)
        userService.hideLessonById(matheLessons.first().id)

        val userLessons = userService.buildUserLessons()
        val userDays = userService.buildUserCalenderDays()

        assertEquals(2, repository.getLessonsByTitleAndGroupsCode("Mathe", "mb-MBB_4").size)
        assertEquals(2, userLessons.size)
        assertEquals(listOf("Mathe", "Informatik"), userLessons.map { it.title })
        assertEquals(listOf("2026-05-19", "2026-05-20"), userDays.map { it.date })
        assertTrue(userDays.any { day -> day.events.isNotEmpty() })
    }

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
