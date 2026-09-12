package com.example.timetable

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import com.example.timetable.data.local.db.TimetableDatabase
import com.example.timetable.data.remote.DaVinciApi
import com.example.timetable.data.services.UserTimetableService
import kotlinx.coroutines.flow.first
import com.example.timetable.viewmodel.TimetableViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
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
        val userService = UserTimetableService(repository, preferencesStore, database)
        val matheLessons = repository.getLessonsByTitleAndGroupsCode("Mathe", "mb-MBB_4").sortedBy { it.date }
        val informatikLesson = repository.getLessonsByTitleAndGroupsCode("Informatik", "eti-SKIB_4").first()

        userService.setGroupsCode("mb-MBB_4")
        userService.setSetupComplete(true)
        userService.addExtraLessonById(informatikLesson.id)
        userService.hideLessonById(matheLessons.first().id)

        val userLessons = userService.userLessonsFlow().first()
        val userDays = userService.userCalenderDaysFlow().first()
        val preferences = userService.getPreferences()

        assertTrue(preferences.isSetupComplete)
        assertEquals(2, repository.getLessonsByTitleAndGroupsCode("Mathe", "mb-MBB_4").size)
        assertEquals(2, userLessons.size)
        assertEquals(listOf("Mathe", "Informatik"), userLessons.map { it.title })
        assertEquals(listOf("2026-05-19", "2026-05-20"), userDays.map { it.date })
        assertTrue(userDays.any { day -> day.events.isNotEmpty() })

        database.close()
    }

    @Test
    fun buildUserLessons_prefersHiddenRule_whenLessonIsExtraAndHidden() = runBlocking {
        val tempDir = Files.createTempDirectory("user-timetable-service-conflict-test").toFile()
        val database = createDatabase()
        val repository = TimetableRepository(
            context = applicationContext(),
            api = DaVinciApi(downloader = { sampleJson() }),
            database = database
        )
        repository.initialize()

        val preferencesStore = createPreferencesStore(tempDir)
        val userService = UserTimetableService(repository, preferencesStore, database)
        val informatikLesson = repository.getLessonsByTitleAndGroupsCode("Informatik", "eti-SKIB_4").first()

        userService.setGroupsCode("mb-MBB_4") // Need a groupsCode to trigger the flow
        userService.addExtraLessonById(informatikLesson.id)
        userService.hideLessonById(informatikLesson.id)

        val userLessons = userService.userLessonsFlow().first()

        assertTrue(userLessons.isEmpty() || userLessons.none { it.id == informatikLesson.id })

        database.close()
    }

    @Test
    fun buildUserLessons_usesCourseWideExtraAndHiddenLessons() = runBlocking {
        val tempDir = Files.createTempDirectory("user-timetable-service-coursewide-test").toFile()
        val database = createDatabase()
        val repository = TimetableRepository(
            context = applicationContext(),
            api = DaVinciApi(downloader = { sampleJson() }),
            database = database
        )
        repository.initialize()

        val preferencesStore = createPreferencesStore(tempDir)
        val userService = UserTimetableService(repository, preferencesStore, database)

        userService.setGroupsCode("mb-MBB_4")
        userService.setSetupComplete(true)
        userService.addExtraLesson("mb-MBB_4", "Informatik")

        var userLessons = userService.userLessonsFlow().first()
        assertEquals(3, userLessons.size) // 2 Mathe + 1 Informatik
        assertTrue(userLessons.any { it.title == "Informatik" })

        userService.hideLesson("mb-MBB_4", "Mathe")

        userLessons = userService.userLessonsFlow().first()
        assertEquals(1, userLessons.size)
        assertEquals("Informatik", userLessons.first().title)

        database.close()
    }


    @Test
    fun test_preference_toggles_and_emoji_management() = runBlocking {
        val tempDir = Files.createTempDirectory("user-timetable-service-prefs-test").toFile()
        val database = createDatabase()
        val repository = TimetableRepository(
            context = applicationContext(),
            api = DaVinciApi(downloader = { sampleJson() }),
            database = database
        )
        repository.initialize()

        val preferencesStore = createPreferencesStore(tempDir)
        val userService = UserTimetableService(repository, preferencesStore, database)

        // Verify default alert settings
        var prefs = userService.getPreferences()
        assertTrue(prefs.isCancellationAlertEnabled)
        assertTrue(prefs.isRoomChangeAlertEnabled)
        assertTrue(!prefs.isDynamicColorEnabled)
        assertTrue(prefs.moduleEmojis.isEmpty())

        // Change alert preferences
        userService.setCancellationAlertEnabled(false)
        userService.setRoomChangeAlertEnabled(false)
        userService.setDynamicColorEnabled(true)

        prefs = userService.getPreferences()
        assertTrue(!prefs.isCancellationAlertEnabled)
        assertTrue(!prefs.isRoomChangeAlertEnabled)
        assertTrue(prefs.isDynamicColorEnabled)

        // Manage emojis
        userService.updateModuleEmoji("Mathe", "📐")
        userService.updateModuleEmoji("Informatik", "💻")

        prefs = userService.getPreferences()
        assertEquals(2, prefs.moduleEmojis.size)
        assertEquals("📐", prefs.moduleEmojis["Mathe"])
        assertEquals("💻", prefs.moduleEmojis["Informatik"])

        // Remove an emoji
        userService.removeModuleEmoji("Mathe")
        prefs = userService.getPreferences()
        assertEquals(1, prefs.moduleEmojis.size)
        assertEquals("💻", prefs.moduleEmojis["Informatik"])
        assertTrue(!prefs.moduleEmojis.containsKey("Mathe"))

        database.close()
    }


    @Test
    fun collectingTimetableDoesNotUpdatePreferences() = runBlocking {
        val tempDir = Files.createTempDirectory("user-timetable-icons-test").toFile()
        val database = createDatabase()
        try {
            val repository = TimetableRepository(
                context = applicationContext(),
                api = DaVinciApi(downloader = { sampleJson() }),
                database = database
            )
            repository.initialize()
            var updates = 0
            val preferencesStore = createPreferencesStore(tempDir) { updates++ }
            val service = UserTimetableService(repository, preferencesStore, database)
            service.completeSetup("mb-MBB_4")
            updates = 0

            val results = List(3) { async { service.userLessonsFlow().first() } }.awaitAll()
            assertTrue(results.all { it.size == 2 })
            assertTrue(service.getPreferences().moduleEmojis.isEmpty())
            assertEquals(0, updates)

            service.updateModuleEmoji("Mathe", "Calculate")
            service.addExtraLessonById(repository.getAllLessons().first { it.title == "Informatik" }.id)
            val savedPreferences = service.getPreferences()
            updates = 0
            repository.reloadJson()
            service.userCalenderDaysFlow().first()
            UserTimetableService(repository, preferencesStore, database).userLessonsFlow().first()

            assertEquals(savedPreferences, service.getPreferences())
            assertEquals(0, updates)
        } finally {
            database.close()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun timetableViewModelStartsWithCachedCustomIcons() = runBlocking {
        val database = createDatabase()
        val viewModels = ViewModelStore()
        Dispatchers.setMain(StandardTestDispatcher())
        try {
            val repository = TimetableRepository(
                context = applicationContext(),
                api = DaVinciApi(downloader = { sampleJson() }),
                database = database
            )
            val tempDir = Files.createTempDirectory("user-timetable-cached-icons-test").toFile()
            val preferencesStore = createPreferencesStore(tempDir)
            val service = UserTimetableService(repository, preferencesStore, database)
            service.completeSetup("mb-MBB_4")
            service.updateModuleEmoji("Mathe", "Calculate")
            val savedPreferences = preferencesStore.load()

            val viewModel = TimetableViewModel(repository, service)
            viewModels.put("timetable", viewModel)
            // No coroutines have run yet.
            assertEquals(savedPreferences, viewModel.preferences.value)
        } finally {
            viewModels.clear()
            Dispatchers.resetMain()
            database.close()
        }
    }

    @Test
    fun activeTimetableDoesNotWriteIconsWhenLessonsArriveAfterSetup() = runBlocking {
        val tempDir = Files.createTempDirectory("user-timetable-delayed-icons-test").toFile()
        val database = createDatabase()
        try {
            val repository = TimetableRepository(
                context = applicationContext(),
                api = DaVinciApi(downloader = { sampleJson() }),
                database = database
            )
            val service = UserTimetableService(repository, createPreferencesStore(tempDir), database)
            service.completeSetup("mb-MBB_4")
            assertTrue(service.userLessonsFlow().first().isEmpty())
            assertTrue(service.getPreferences().moduleEmojis.isEmpty())

            withTimeout(10_000) {
                val loadedLessons = async { service.userLessonsFlow().first { it.isNotEmpty() } }
                repository.initialize()
                assertEquals(2, loadedLessons.await().size)
            }
            assertTrue(service.getPreferences().moduleEmojis.isEmpty())
        } finally {
            database.close()
        }
    }

    private fun createPreferencesStore(
        tempDir: java.io.File,
        onUpdate: () -> Unit = {}
    ): UserSchedulePreferencesStore {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempDir.resolve("user-timetable.preferences_pb") }
        )
        return UserSchedulePreferencesStore(object : DataStore<Preferences> by dataStore {
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                onUpdate()
                return dataStore.updateData(transform)
            }
        })
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
