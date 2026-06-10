package com.example.timetable.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.timetable.view.screens.TimetableScreen
import com.example.timetable.view.theme.TimeTableTheme
import org.junit.Rule
import org.junit.Test

class TimetableScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun timetableScreen_displaysCourseNameAndLessons() {
        val testCourse = "eti-Auton. Mob. Syst. Vorl."
        
        composeTestRule.setContent {
            TimeTableTheme {
                TimetableScreen(courseName = testCourse)
            }
        }

        // Check if header is displayed
        composeTestRule.onNodeWithText("Dein Plan", substring = true).assertIsDisplayed()
        
        // Check if a lesson card with the title is displayed
        composeTestRule.onNodeWithText("eti-Autonome Mobile Systeme Vorl.", substring = true).assertIsDisplayed()
    }

    @Test
    fun timetableScreen_opensOverlayOnLessonSelection() {
        val testCourse = "eti-Auton. Mob. Syst. Vorl."
        
        composeTestRule.setContent {
            TimeTableTheme {
                TimetableScreen(courseName = testCourse)
            }
        }

        // Click on the lesson card
        composeTestRule.onNodeWithText("eti-Autonome Mobile Systeme Vorl.").performClick()
        
        // Check if overlay header is displayed
        composeTestRule.onNodeWithText("Vorlesungsdetails").assertIsDisplayed()
    }
}
