package com.example.timetable.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timetable.data.local.db.TimetableDatabase
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import com.example.timetable.data.local.preferences.userSchedulePreferencesDataStore
import com.example.timetable.data.remote.DaVinciApi
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.UserTimetableService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.timetable.utils.enums.Faculty
import com.example.timetable.view.screens.InitialSetupScreen
import com.example.timetable.viewmodel.InitialSetupViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Test für den InitialSetupScreen.
 * Verwendet Compose Test Library.
 */
@RunWith(AndroidJUnit4::class)
class InitialSetupScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
        InitialSetupViewModel(repository, userService)
    }

    @Before
    fun setUp() {
        runBlocking {
            preferencesStore.clear()
            database.userRulesDao().clearExtra()
            database.userRulesDao().clearHidden()
            repository.reloadJson()
        }
    }

    @Test
    fun initialSetupScreen_displaysCorrectly() {
        composeTestRule.setContent {
            InitialSetupScreen(
                viewModel = viewModel,
                onNavigateToTimetable = { }
            )
        }

        composeTestRule.onNodeWithText("HOSTvinci").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dein übersichtlicher Stundenplan für die HOST.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Fakultät auswählen").assertIsDisplayed()

        Faculty.entries.forEach { faculty ->
            composeTestRule.onNodeWithText(faculty.label).assertIsDisplayed()
        }
    }

    @Test
    fun facultyChip_selectionWorks() {
        composeTestRule.setContent {
            InitialSetupScreen(
                viewModel = viewModel,
                onNavigateToTimetable = { }
            )
        }

        val targetFaculty = Faculty.ETI
        composeTestRule.onNodeWithText(targetFaculty.label).performClick()

        // Verifiziert den Wechsel in die Studiengang-Ansicht
        composeTestRule.onNodeWithText(targetFaculty.label).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Zurück zur Fakultätsauswahl")
            .assertIsDisplayed()

        // Suchfeld sollte nun sichtbar sein
        composeTestRule.onNodeWithTag("courseSearchField").assertIsDisplayed()
    }

    @Test
    fun initialSetupScreen_onFirstStart_searchIsHidden() {
        composeTestRule.setContent {
            InitialSetupScreen(
                viewModel = viewModel,
                onNavigateToTimetable = { }
            )
        }

        // Am Anfang sollte das Suchfeld nicht existieren
        composeTestRule.onNodeWithTag("courseSearchField").assertDoesNotExist()
    }

    @Test
    fun initialSetupScreen_selectionAndContinue_triggersNavigation() {
        var navigationCalled = false
        composeTestRule.setContent {
            InitialSetupScreen(
                viewModel = viewModel,
                onNavigateToTimetable = { navigationCalled = true }
            )
        }

        // 1. Fakultät wählen
        val faculty = Faculty.WS
        composeTestRule.onNodeWithText(faculty.label).performClick()

        // 2. Suche bedienen
        composeTestRule.onNodeWithTag("courseSearchField").performTextInput("Stat")

        // 3. Studiengang auswählen (neu benötigt in der UI)
        composeTestRule.onNodeWithText("ws-Stat I Ü").performClick()

        // 4. Den "Stundenplan anzeigen" Button klicken
        composeTestRule.onNodeWithTag("setupContinueButton").performClick()

        composeTestRule.waitForIdle()
        assert(navigationCalled)
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
                    }
                  ],
                  "eventTimes": []
                }
              }
            }
        """.trimIndent()
    }
}
