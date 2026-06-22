package com.example.timetable.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timetable.data.local.db.TimetableDatabase
import com.example.timetable.data.local.preferences.UserSchedulePreferences
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import com.example.timetable.data.local.preferences.userSchedulePreferencesDataStore
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.UserTimetableService
import com.example.timetable.view.screens.CourseSelectionScreen
import com.example.timetable.view.theme.TimeTableTheme
import com.example.timetable.viewmodel.CourseSelectionViewModel
import com.example.timetable.data.remote.DaVinciApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourseSelectionScreenTest {

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
        CourseSelectionViewModel(repository, userService)
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
    fun courseSelectionScreen_initialState_showsNoResults() {
        composeTestRule.setContent {
            TimeTableTheme {
                CourseSelectionScreen(viewModel = viewModel)
            }
        }

        // Initially header and search field should be visible
        composeTestRule.onNodeWithText("Modul wählen").assertIsDisplayed()

        // No results should be displayed since query length is 0
        composeTestRule.onNodeWithText("Kostenrechnung", substring = true).assertDoesNotExist()
    }

    @Test
    fun courseSelectionScreen_searchAndSelectModule_showsNoConflictPreview() {
        runBlocking {
            preferencesStore.save(
                UserSchedulePreferences(
                    isSetupComplete = true,
                    groupsCode = "ws-Stat I Ü"
                )
            )
        }

        var backCalled = false

        composeTestRule.setContent {
            TimeTableTheme {
                CourseSelectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { backCalled = true }
                )
            }
        }

        // Type search query
        composeTestRule.onNodeWithText("Name, Dozent, Raum...").performTextInput("Kostenrechnung")

        // Result card should be displayed
        val moduleTitle = "mb-Kostenrechnung und Kostenanalyse - ÜB"
        composeTestRule.onNodeWithText(moduleTitle).assertIsDisplayed()

        // Click to select the module
        composeTestRule.onNodeWithText(moduleTitle).performClick()

        // Verify conflict-free preview panel is shown
        composeTestRule.onNodeWithText("Modul hinzufügen?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keine Konflikte mit deinem aktuellen Plan.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("In den Plan aufnehmen").assertIsDisplayed()

        // Click Add Button
        composeTestRule.onNodeWithText("In den Plan aufnehmen").performClick()

        // Check if module was added to DB extra rules and back navigation triggered
        val extras = runBlocking { database.userRulesDao().getExtraLessons() }
        assertTrue(extras.any { it.title == moduleTitle })
        assertTrue(backCalled)
    }

    @Test
    fun courseSelectionScreen_selectedModuleConflicts_showsWarningPreview() {
        runBlocking {
            // Enrolled in ETI course, which includes the conflicting "Conflict Course"
            preferencesStore.save(
                UserSchedulePreferences(
                    isSetupComplete = true,
                    groupsCode = "eti-Auton. Mob. Syst. Vorl."
                )
            )
        }

        composeTestRule.setContent {
            TimeTableTheme {
                CourseSelectionScreen(viewModel = viewModel)
            }
        }

        // Type search query for the same module
        composeTestRule.onNodeWithText("Name, Dozent, Raum...").performTextInput("Kostenrechnung")

        // Click to select the result
        val moduleTitle = "mb-Kostenrechnung und Kostenanalyse - ÜB"
        composeTestRule.onNodeWithText(moduleTitle).performClick()

        // Verify conflict preview is shown with warning
        composeTestRule.onNodeWithText("Modul hinzufügen?").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Achtung:", substring = true)[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText(
            "Kollision mit Conflict Course",
            substring = true
        )[0].assertIsDisplayed()

        // Add button should change text to force add
        composeTestRule.onNodeWithText("Trotzdem In den Plan aufnehmen").assertIsDisplayed()
    }

    @Test
    fun courseSelectionScreen_searchByTeacher_showsResult() {
        composeTestRule.setContent {
            TimeTableTheme {
                CourseSelectionScreen(viewModel = viewModel)
            }
        }

        // Type search query for teacher code
        composeTestRule.onNodeWithText("Name, Dozent, Raum...").performTextInput("Garske")

        // Result card should be displayed (corresponds to eti-Autonome Mobile Systeme Vorl.)
        val moduleTitle = "eti-Autonome Mobile Systeme Vorl."
        composeTestRule.onNodeWithText(moduleTitle).assertIsDisplayed()
    }

    @Test
    fun courseSelectionScreen_searchByRoom_showsResult() {
        composeTestRule.setContent {
            TimeTableTheme {
                CourseSelectionScreen(viewModel = viewModel)
            }
        }

        // Type search query for room code
        composeTestRule.onNodeWithText("Name, Dozent, Raum...").performTextInput("4/221")

        // Result card should be displayed
        val moduleTitle = "eti-Autonome Mobile Systeme Vorl."
        composeTestRule.onNodeWithText(moduleTitle).assertIsDisplayed()
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