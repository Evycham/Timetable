package com.example.timetable.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.timetable.view.screens.TimetableScreen
import com.example.timetable.view.theme.TimeTableTheme
import com.example.timetable.view.json.MockLogic
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TimetableScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        MockLogic.selectedModuleTitles.clear()
        MockLogic.moduleEmojis.clear()
    }

    @Test
    fun timetableScreen_displaysCourseNameAndLessons() {
        // "eti-Auton. Mob. Syst. Vorl." has lessons in test-short.json
        val testCourse = "eti-Auton. Mob. Syst. Vorl."
        
        // PRE-POPULATE MockLogic so lessons actually show up
        MockLogic.selectedModuleTitles.add("eti-Autonome Mobile Systeme Vorl.")
        
        composeTestRule.setContent {
            TimeTableTheme {
                TimetableScreen(courseName = testCourse)
            }
        }

        // Check if navigation icon is displayed instead of the title which was removed
        composeTestRule.onNodeWithContentDescription("Zurück").assertIsDisplayed()
        
        // Check if the lesson card is displayed (using substring without prefix to work in all views)
        composeTestRule.onNodeWithText("Autonome Mobile Systeme Vorl.", substring = true).assertIsDisplayed()
    }

    @Test
    fun timetableScreen_opensOverlayOnLessonSelection() {
        val testCourse = "eti-Auton. Mob. Syst. Vorl."
        
        // PRE-POPULATE MockLogic
        MockLogic.selectedModuleTitles.add("eti-Autonome Mobile Systeme Vorl.")
        
        composeTestRule.setContent {
            TimeTableTheme {
                TimetableScreen(courseName = testCourse)
            }
        }

        // Click on the lesson card (using substring without prefix)
        composeTestRule.onNodeWithText("Autonome Mobile Systeme Vorl.", substring = true).performClick()
        
        // Check if overlay header is displayed
        composeTestRule.onNodeWithText("Vorlesungsdetails").assertIsDisplayed()
    }

    @Test
    fun timetableScreen_emptyState_showsNoLessonsMessage() {
        val testCourse = "non-existent-course"

        composeTestRule.setContent {
            TimeTableTheme {
                TimetableScreen(courseName = testCourse)
            }
        }

        // Daily view pager page should render empty state
        composeTestRule.onNodeWithText("Keine Vorlesungen geplant.").assertIsDisplayed()
    }
}
