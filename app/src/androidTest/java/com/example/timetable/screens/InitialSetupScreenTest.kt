package com.example.timetable.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timetable.utils.enums.Faculty
import com.example.timetable.view.screens.InitialSetupScreen
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

    @Test
    fun initialSetupScreen_displaysCorrectly() {
        composeTestRule.setContent {
            InitialSetupScreen(onNavigateToTimetable = { })
        }

        composeTestRule.onNodeWithText("HOSTvinci").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dein übersichtlicher Stundenplan für die HOST.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fakultät auswählen").assertIsDisplayed()
        
        Faculty.entries.forEach { faculty ->
            composeTestRule.onNodeWithText(faculty.label).assertIsDisplayed()
        }
    }

    @Test
    fun facultyChip_selectionWorks() {
        composeTestRule.setContent {
            InitialSetupScreen(onNavigateToTimetable = { })
        }

        val targetFaculty = Faculty.ETI
        composeTestRule.onNodeWithText(targetFaculty.label).performClick()

        // Verifiziert den Wechsel in die Studiengang-Ansicht
        composeTestRule.onNodeWithText(targetFaculty.label).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Zurück zur Fakultätsauswahl").assertIsDisplayed()
        
        // Suchfeld sollte nun sichtbar sein
        composeTestRule.onNodeWithTag("courseSearchField").assertIsDisplayed()
    }

    @Test
    fun initialSetupScreen_onFirstStart_searchIsHidden() {
        composeTestRule.setContent {
            InitialSetupScreen(onNavigateToTimetable = { })
        }

        // Am Anfang sollte das Suchfeld nicht existieren
        composeTestRule.onNodeWithTag("courseSearchField").assertDoesNotExist()
    }

    @Test
    fun initialSetupScreen_selectionAndContinue_triggersNavigation() {
        var navigationCalled = false
        composeTestRule.setContent {
            InitialSetupScreen(onNavigateToTimetable = { navigationCalled = true })
        }

        // 1. Fakultät wählen
        val faculty = Faculty.WS
        composeTestRule.onNodeWithText(faculty.label).performClick()
        
        // 2. Suche bedienen
        composeTestRule.onNodeWithTag("courseSearchField").performTextInput("WIB")

        // 3. Studiengang auswählen (neu benötigt in der UI)
        composeTestRule.onNodeWithText("${faculty.prefix} - Bachelor (B.Sc.)").performClick()

        // 4. Den "Stundenplan anzeigen" Button klicken
        composeTestRule.onNodeWithTag("setupContinueButton").performClick()

        assert(navigationCalled)
    }
}
