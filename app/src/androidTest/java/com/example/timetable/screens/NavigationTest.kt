package com.example.timetable.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timetable.data.local.preferences.UserSchedulePreferences
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import com.example.timetable.data.local.preferences.userSchedulePreferencesDataStore
import com.example.timetable.view.navigation.TimetableNavHost
import com.example.timetable.view.theme.TimeTableTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Test für die Navigation in der NavGraph.kt.
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val preferencesStore =
        UserSchedulePreferencesStore(context.userSchedulePreferencesDataStore)

    @Before
    fun setUp() = runBlocking {
        // Reset preferences before each test
        preferencesStore.clear()
    }

    @Test
    fun startApp_onFirstStart_showsInitialSetupScreen() {
        composeTestRule.setContent {
            TimeTableTheme {
                TimetableNavHost(rememberNavController())
            }
        }

        // Home route should trigger and navigate to InitialSetup
        composeTestRule.onNodeWithText("Fakultät auswählen").assertIsDisplayed()
        composeTestRule.onNodeWithText("HOSTvinci").assertIsDisplayed()
    }

    @Test
    fun startApp_withSetupComplete_showsTimetableScreen() = runBlocking {
        // Simulate setup complete
        val testCourse = "eti-SKIB_4"
        preferencesStore.save(
            UserSchedulePreferences(
                isSetupComplete = true,
                groupsCode = testCourse
            )
        )

        composeTestRule.setContent {
            TimeTableTheme {
                TimetableNavHost(rememberNavController())
            }
        }

        // Home route should trigger and navigate to TimetableScreen
        // Since we don't have real data in tests easily, we check for a TimetableScreen indicator
        // Based on TimetableScreen.kt, it shows NavigationBar with "Heute" and "Woche"
        composeTestRule.onNodeWithText("Heute").assertIsDisplayed()
        composeTestRule.onNodeWithText("Woche").assertIsDisplayed()
    }
}
