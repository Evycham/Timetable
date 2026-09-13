package com.example.timetable.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.timetable.data.local.db.TimetableDatabase
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import com.example.timetable.data.remote.DaVinciApi
import com.example.timetable.data.remote.LessonParser
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.AlertType
import com.example.timetable.data.services.TimetableAlertDetector
import com.example.timetable.data.services.UserTimetableService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNotificationManager
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@RunWith(RobolectricTestRunner::class)
class NotificationPipelineIntegrationTest {
    private lateinit var context: Context
    private lateinit var database: TimetableDatabase
    private lateinit var tempDir: File
    private lateinit var preferencesStore: UserSchedulePreferencesStore
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, TimetableDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tempDir = Files.createTempDirectory("test-notification-pipeline").toFile()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tempDir, "user_prefs.preferences_pb") }
        )
        preferencesStore = UserSchedulePreferencesStore(dataStore)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
    }

    @After
    fun tearDown() {
        database.close()
        tempDir.deleteRecursively()
    }

    @Test
    fun testRoomChangeDetectionAndNotification() = runTest {
        val initialJson = createDavinciJson(
            room = "4/302",
            isCancelled = false
        )
        val changedRoomJson = createDavinciJson(
            room = "Dachboden",
            isCancelled = false
        )

        val repo = TimetableRepository(
            context = context,
            api = DaVinciApi(downloader = { initialJson }),
            database = database
        )
        val userService = UserTimetableService(repo, preferencesStore, database)
        userService.setGroupsCode("mb-MBB_4")
        userService.setSetupComplete(true)

        // initial sync
        repo.reloadJson()
        val oldLessons = userService.userLessonsFlow().first()
        assertEquals(1, oldLessons.size)
        assertEquals(setOf("4/302"), oldLessons.first().rooms)

        // bg update with new data
        val detector = TimetableAlertDetector()
        val parser = LessonParser()
        val newParsedLessons = parser.parseLessons(
            JSONObject(changedRoomJson)
                .getJSONObject("result")
                .getJSONObject("displaySchedule")
                .optJSONArray("lessonTimes")
        )

        val alerts = detector.detectAlerts(
            oldUserLessons = oldLessons,
            newLessons = newParsedLessons,
            userGroupsCode = "mb-MBB_4",
            extraLessons = emptyList(),
            hiddenLessons = emptyList()
        )

        assertEquals("Should detect 1 room change alert", 1, alerts.size)
        assertEquals(AlertType.ROOM_CHANGE, alerts.first().type)
        assertTrue(alerts.first().detail.contains("4/302"))
        assertTrue(alerts.first().detail.contains("Dachboden"))
    }

    @Test
    fun testCancellationDetection_withExplicitChangesBlock() = runTest {
        val initialJson = createDavinciJson(
            room = "4/302",
            isCancelled = false
        )

        val cancelledJson = createDavinciJson(
            room = "4/302",
            isCancelled = true
        )

        val repo = TimetableRepository(
            context = context,
            api = DaVinciApi(downloader = { initialJson }),
            database = database
        )
        val userService = UserTimetableService(repo, preferencesStore, database)
        userService.setGroupsCode("mb-MBB_4")

        repo.reloadJson()
        val oldLessons = userService.userLessonsFlow().first()

        val parser = LessonParser()
        val newParsedLessons = parser.parseLessons(
            JSONObject(cancelledJson)
                .getJSONObject("result")
                .getJSONObject("displaySchedule")
                .optJSONArray("lessonTimes")
        )

        val detector = TimetableAlertDetector()
        val alerts = detector.detectAlerts(
            oldUserLessons = oldLessons,
            newLessons = newParsedLessons,
            userGroupsCode = "mb-MBB_4",
            extraLessons = emptyList(),
            hiddenLessons = emptyList()
        )

        assertEquals("Should detect 1 cancellation alert", 1, alerts.size)
        assertEquals(AlertType.CANCELLATION, alerts.first().type)
    }

    @Test
    fun testPreferencesFilter_disablesRoomChangeNotifications() = runTest {
        val userService = UserTimetableService(
            TimetableRepository(context, database = database),
            preferencesStore,
            database
        )
        userService.setRoomChangeAlertEnabled(false)
        userService.setCancellationAlertEnabled(true)

        val prefs = userService.getPreferences()
        assertFalse(prefs.isRoomChangeAlertEnabled)
        assertTrue(prefs.isCancellationAlertEnabled)
    }

    // helper function to create test Data
    private fun createDavinciJson(room: String, isCancelled: Boolean): String {
        val dateString = LocalDate.now().plusDays(7).format(DateTimeFormatter.BASIC_ISO_DATE)

        val changesBlock = if (isCancelled) {
            """
                "changes": {
                    "caption": "Keine Vertretung",
                    "reasonType": "teacherAbsence",
                    "cancelled": "substitutionCancelled",
                    "modified": "20260912"
                    }
            """.trimIndent()
        } else {
            """
                "changes": null
            """.trimIndent()
        }

        return """
            {
                "result": {
                    "displaySchedule": {
                        "lessonTimes": [
                            {
                                "courseTitle": "Mathematik XIV",
                                "dates": ["$dateString"],
                                "startTime": "0945",
                                "endTime": "1115",
                                "classCodes": ["mb-MBB_4"],
                                "teacherCodes": ["mb-Mueller"],
                                "roomCodes": ["$room"],
                                $changesBlock
                            }
                        ],
                        "eventTimes": []
                    }
                }
            }
        """.trimIndent()
    }
}