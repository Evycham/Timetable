package com.example.timetable.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timetable.data.local.db.TimetableDatabase
import com.example.timetable.data.local.preferences.UserSchedulePreferences
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import com.example.timetable.data.local.preferences.userSchedulePreferencesDataStore
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.UserTimetableService
import com.example.timetable.view.screens.TimetableScreen
import com.example.timetable.view.theme.TimeTableTheme
import com.example.timetable.viewmodel.TimetableViewModel
import com.example.timetable.data.remote.DaVinciApi
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimetableScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val database = TimetableDatabase.getInstance(context)
    private val preferencesStore =
        UserSchedulePreferencesStore(context.userSchedulePreferencesDataStore)
    private val repository by lazy {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val json = TEST_JSON
            .replace("20260615", todayStr)
            .replace("20260616", todayStr)
            .replace("20260617", todayStr)
        TimetableRepository(
            context = context,
            api = DaVinciApi(downloader = { json }),
            database = database
        )
    }
    private val userService by lazy {
        UserTimetableService(repository, preferencesStore, database)
    }
    private val viewModel by lazy {
        TimetableViewModel(repository, userService)
    }

    @Before
    fun setUp() {
        runBlocking {
            // Reset preferences and rules
            preferencesStore.clear()
            database.userRulesDao().clearExtra()
            database.userRulesDao().clearHidden()

            // Reload test data into DB
            repository.reloadJson()
        }
    }

    @Test
    fun timetableScreen_displaysCourseNameAndLessons() {
        val testCourse = "eti-Auton. Mob. Syst. Vorl."

        runBlocking {
            preferencesStore.save(
                UserSchedulePreferences(
                    isSetupComplete = true,
                    groupsCode = testCourse
                )
            )
        }

        composeTestRule.setContent {
            TimeTableTheme {
                TimetableScreen(
                    courseName = testCourse,
                    viewModel = viewModel
                )
            }
        }

        // Switch to Daily View to see the Lesson Cards
        composeTestRule.onNodeWithText("Heute").performClick()

        // Check if the lesson card is displayed (using substring without prefix to work in all views)
        composeTestRule.onNodeWithText("Autonome Mobile Systeme Vorl.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun timetableScreen_opensOverlayOnLessonSelection() {
        val testCourse = "eti-Auton. Mob. Syst. Vorl."

        runBlocking {
            preferencesStore.save(
                UserSchedulePreferences(
                    isSetupComplete = true,
                    groupsCode = testCourse
                )
            )
        }

        composeTestRule.setContent {
            TimeTableTheme {
                TimetableScreen(
                    courseName = testCourse,
                    viewModel = viewModel
                )
            }
        }

        // Switch to Daily View to see the Lesson Cards
        composeTestRule.onNodeWithText("Heute").performClick()

        // Click on the lesson card (using substring without prefix)
        composeTestRule.onNodeWithText("Autonome Mobile Systeme Vorl.", substring = true)
            .performClick()

        // Check if overlay header is displayed
        composeTestRule.onNodeWithText("Vorlesungsdetails").assertIsDisplayed()
    }

    @Test
    fun timetableScreen_emptyState_showsNoLessonsMessage() {
        val testCourse = "non-existent-course"

        runBlocking {
            preferencesStore.save(
                UserSchedulePreferences(
                    isSetupComplete = true,
                    groupsCode = testCourse
                )
            )
        }

        composeTestRule.setContent {
            TimeTableTheme {
                TimetableScreen(
                    courseName = testCourse,
                    viewModel = viewModel
                )
            }
        }

        // Switch to Daily View to see empty state
        composeTestRule.onNodeWithText("Heute").performClick()

        // Daily view pager page should render empty state
        composeTestRule.onNodeWithText("Keine Vorlesungen geplant.").assertIsDisplayed()
    }

    companion object {
        private val TEST_JSON = """
            {
              "result": {
                "displaySchedule": {
                  "lessonTimes": [
                    {
                      "courseTitle": "eti-Autonome Mobile Systeme Vorl.",
                      "dates": ["20260615"],
                      "startTime": "1015",
                      "endTime": "1145",
                      "classCodes": ["eti-Auton. Mob. Syst. Vorl."],
                      "teacherCodes": ["eti-Garske"],
                      "roomCodes": ["4/221 (H4)"]
                    },
                    {
                      "courseTitle": "mb-Kostenrechnung und Kostenanalyse - ÜB",
                      "dates": ["20260616"],
                      "startTime": "0945",
                      "endTime": "1115",
                      "classCodes": ["mb-SPB_4", "eti-WETB 4", "mb-WIB_4"],
                      "teacherCodes": ["mb-Türr"],
                      "roomCodes": ["4/206 (H4)"]
                    },
                    {
                      "courseTitle": "ws-Statistik I Übung",
                      "dates": ["20260617"],
                      "startTime": "1400",
                      "endTime": "1530",
                      "classCodes": ["ws-Stat I Ü"],
                      "teacherCodes": ["ws-Honekamp"],
                      "roomCodes": ["21/202 (H21)"]
                    },
                    {
                      "courseTitle": "Conflict Course",
                      "dates": ["20260616"],
                      "startTime": "0945",
                      "endTime": "1115",
                      "classCodes": ["eti-Auton. Mob. Syst. Vorl."],
                      "teacherCodes": ["eti-ConflictDocent"],
                      "roomCodes": ["4/100"]
                    }
                  ],
                  "eventTimes": []
                }
              }
            }
        """.trimIndent()
    }
}
