package com.example.timetable.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.timetable.view.json.MockLogic
import com.example.timetable.view.screens.CourseSelectionScreen
import com.example.timetable.view.theme.TimeTableTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CourseSelectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        MockLogic.selectedModuleTitles.clear()
        MockLogic.moduleEmojis.clear()
    }

    @Test
    fun courseSelectionScreen_initialState_showsNoResults() {
        composeTestRule.setContent {
            TimeTableTheme {
                CourseSelectionScreen()
            }
        }

        // Initially header and search field should be visible
        composeTestRule.onNodeWithText("Modul wählen").assertIsDisplayed()
        
        // No results should be displayed since query length is 0
        composeTestRule.onNodeWithText("Kostenrechnung", substring = true).assertDoesNotExist()
    }

    @Test
    fun courseSelectionScreen_searchAndSelectModule_showsNoConflictPreview() {
        var backCalled = false

        composeTestRule.setContent {
            TimeTableTheme {
                CourseSelectionScreen(onNavigateBack = { backCalled = true })
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
        composeTestRule.onNodeWithText("Keine Konflikte mit deinem aktuellen Plan.").assertIsDisplayed()
        composeTestRule.onNodeWithText("In den Plan aufnehmen").assertIsDisplayed()

        // Click Add Button
        composeTestRule.onNodeWithText("In den Plan aufnehmen").performClick()

        // Check if module was added to MockLogic and back navigation triggered
        assertTrue(MockLogic.selectedModuleTitles.contains(moduleTitle))
        assertTrue(backCalled)
    }

    @Test
    fun courseSelectionScreen_selectedModuleConflicts_showsWarningPreview() {
        // Pre-populate MockLogic to create a conflict on date/time of "mb-Kostenrechnung und Kostenanalyse - ÜB"
        val moduleTitle = "mb-Kostenrechnung und Kostenanalyse - ÜB"
        MockLogic.selectedModuleTitles.add(moduleTitle)

        composeTestRule.setContent {
            TimeTableTheme {
                CourseSelectionScreen()
            }
        }

        // Type search query for the same module
        composeTestRule.onNodeWithText("Name, Dozent, Raum...").performTextInput("Kostenrechnung")

        // Click to select the result
        composeTestRule.onNodeWithText(moduleTitle).performClick()

        // Verify conflict preview is shown with warning
        composeTestRule.onNodeWithText("Modul hinzufügen?").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Achtung:", substring = true)[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Kollision mit $moduleTitle", substring = true)[0].assertIsDisplayed()
        
        // Add button should change text to force add
        composeTestRule.onNodeWithText("Trotzdem hinzufügen").assertIsDisplayed()
    }
}