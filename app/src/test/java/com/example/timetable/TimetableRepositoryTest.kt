package com.example.timetable

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.timetable.data.TimetableRepository
import com.example.timetable.data.local.TimetableDatabase
import com.example.timetable.data.services.DaVinciApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TimetableRepositoryTest {

    @Test
    fun initialize_downloadsParsesAndPersists_whenNoLocalDataExists() = runBlocking {
        val database = createDatabase()
        val repository = TimetableRepository(
            context = applicationContext(),
            api = DaVinciApi(downloader = { firstJson() }),
            database = database
        )

        val calenderDays = repository.initialize()

        assertEquals(3, calenderDays.size)
        assertEquals(2, repository.getAllLessons().size)
        assertEquals(1, repository.getAllEvents().size)
        assertEquals(2, repository.getLessonsByGroupsCode("mb-MBB_4").size)

        database.close()
    }

    @Test
    fun initialize_usesExistingDatabase_withoutDownloadingAgain() = runBlocking {
        val database = createDatabase()

        TimetableRepository(
            context = applicationContext(),
            api = DaVinciApi(downloader = { firstJson() }),
            database = database
        ).initialize()

        val repository = TimetableRepository(
            context = applicationContext(),
            api = DaVinciApi(downloader = { error("Downloader should not be used") }),
            database = database
        )

        val calenderDays = repository.initialize()

        assertEquals(3, calenderDays.size)
        assertEquals(2, repository.getAllLessons().size)
        assertEquals(1, repository.getAllEvents().size)

        database.close()
    }

    @Test
    fun loadFromCache_readsExistingDatabase_withoutDownloadingAgain() = runBlocking {
        val database = createDatabase()
        val repository = TimetableRepository(
            context = applicationContext(),
            api = DaVinciApi(downloader = { firstJson() }),
            database = database
        )

        repository.reloadJson()
        repository.clearMemory()

        val calenderDays = repository.loadFromCache()

        assertEquals(3, calenderDays?.size)
        assertEquals(2, repository.getAllLessons().size)
        assertEquals(1, repository.getAllEvents().size)

        database.close()
    }

    @Test
    fun updateMethods_replaceWholeJsonAndRefreshQueries() = runBlocking {
        val database = createDatabase()
        val repository = TimetableRepository(
            context = applicationContext(),
            api = DaVinciApi(downloader = { firstJson() }),
            database = database
        )

        repository.initialize()

        val updateRepository = TimetableRepository(
            context = applicationContext(),
            api = DaVinciApi(downloader = { secondJson() }),
            database = database
        )

        val hasUpdates = updateRepository.updateJsonIfNeeded()

        assertTrue(hasUpdates)
        assertEquals(1, updateRepository.getLessonsByGroupsCode("mb-MBB_4").size)
        assertEquals(1, updateRepository.getLessonsByGroupsCode("eti-SKIB_4").size)
        assertEquals(2, updateRepository.getAllEvents().size)

        val forcedCalenderDays = updateRepository.reloadJson()

        assertFalse(forcedCalenderDays.isEmpty())
        assertEquals(2, updateRepository.getAllEvents().size)

        database.close()
    }

    private fun createDatabase(): TimetableDatabase =
        Room.inMemoryDatabaseBuilder(
            applicationContext(),
            TimetableDatabase::class.java
        ).allowMainThreadQueries().build()

    private fun applicationContext(): Context = ApplicationProvider.getApplicationContext()

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
