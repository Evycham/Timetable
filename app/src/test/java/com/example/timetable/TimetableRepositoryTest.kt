package com.example.timetable

import com.example.timetable.data.services.DaVinciApi
import com.example.timetable.data.TimetableRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class TimetableRepositoryTest {

    @Test
    fun initialize_downloadsAndBuildsMemoryState_whenNoCacheExists() {
        val tempDir = Files.createTempDirectory("timetable-repository-test").toFile()
        val repository = TimetableRepository(
            storageDir = tempDir,
            api = DaVinciApi(downloader = { firstJson() })
        )

        val calenderDays = repository.initialize()

        assertEquals(3, calenderDays.size)
        assertEquals(2, repository.getAllLessons().size)
        assertEquals(1, repository.getAllEvents().size)
        assertEquals(2, repository.getLessonsByGroupsCode("mb-MBB_4").size)
        assertTrue(tempDir.resolve(DaVinciApi.RAW_CACHE_FILE_NAME).exists())
    }

    @Test
    fun loadFromCache_readsExistingRawJson_withoutDownloadingAgain() {
        val tempDir = Files.createTempDirectory("timetable-repository-test").toFile()
        TimetableRepository(
            storageDir = tempDir,
            api = DaVinciApi(downloader = { firstJson() })
        ).initialize()

        val repository = TimetableRepository(
            storageDir = tempDir,
            api = DaVinciApi(downloader = { error("Downloader should not be used") })
        )

        val calenderDays = repository.loadFromCache()

        assertEquals(3, calenderDays?.size)
        assertEquals(2, repository.getAllLessons().size)
        assertEquals(1, repository.getAllEvents().size)
    }

    @Test
    fun updateMethods_replaceWholeJsonAndRefreshQueries() {
        val tempDir = Files.createTempDirectory("timetable-repository-test").toFile()
        val repository = TimetableRepository(
            storageDir = tempDir,
            api = DaVinciApi(downloader = { firstJson() })
        )

        repository.initialize()

        val updateRepository = TimetableRepository(
            storageDir = tempDir,
            api = DaVinciApi(downloader = { secondJson() })
        )

        val hasUpdates = updateRepository.updateJsonIfNeeded()

        assertTrue(hasUpdates)
        assertEquals(1, updateRepository.getLessonsByGroupsCode("mb-MBB_4").size)
        assertEquals(1, updateRepository.getLessonsByGroupsCode("eti-SKIB_4").size)
        assertEquals(2, updateRepository.getAllEvents().size)

        val forcedCalenderDays = updateRepository.reloadJson()

        assertFalse(forcedCalenderDays.isEmpty())
        assertEquals(2, updateRepository.getAllEvents().size)
    }

    private fun firstJson(): String = """
        {
          "result": {
            "displaySchedule": {
              "lessonTimes": [
                {
                  "courseTitle": "Mathe",
                  "dates": ["20260518", "20260519"],
                  "startTime": "0945",
                  "endTime": "1115",
                  "classCodes": ["mb-MBB_4"],
                  "teacherCodes": ["dozent-1"],
                  "roomCodes": ["4/302"],
                  "buildingCodes": ["H4"]
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

    private fun secondJson(): String = """
        {
          "result": {
            "displaySchedule": {
              "lessonTimes": [
                {
                  "courseTitle": "Mathe",
                  "dates": ["20260518"],
                  "startTime": "0945",
                  "endTime": "1115",
                  "classCodes": ["mb-MBB_4"]
                },
                {
                  "courseTitle": "Informatik",
                  "dates": ["20260521"],
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
                },
                {
                  "id": "event-2",
                  "title": "Projektwoche",
                  "startDate": "20260522",
                  "endDate": "20260523",
                  "category": "Block"
                }
              ]
            }
          }
        }
    """.trimIndent()
}
