package com.example.timetable.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.graphics.Color
import com.example.timetable.view.components.timetable.TimetableLessonCard
import com.example.timetable.view.json.JsonLesson
import com.example.timetable.view.json.JsonLessonChange
import com.example.timetable.view.theme.TimeTableTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TimetableLessonCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun timetableLessonCard_displaysAllDetailsAndHandlesClick() {
        val testLesson = JsonLesson(
            id = "lesson-123",
            title = "Mathematik I",
            date = "2026-06-15",
            startTime = "09:45",
            endTime = "11:15",
            room = "4/206 (H4)",
            lecturer = "Dr. Müller",
            course = "eti-INF_2",
            change = JsonLessonChange(
                caption = "Fällt aus",
                message = "Fällt aus"
            )
        )

        var clickCalled = false

        composeTestRule.setContent {
            TimeTableTheme {
                TimetableLessonCard(
                    lesson = testLesson,
                    accentColor = Color.Red,
                    onClick = { clickCalled = true }
                )
            }
        }

        // Verify title
        composeTestRule.onNodeWithText("Mathematik I").assertIsDisplayed()

        // Verify times
        composeTestRule.onNodeWithText("09:45 - 11:15").assertIsDisplayed()

        // Verify room
        composeTestRule.onNodeWithText("4/206").assertIsDisplayed()

        // Verify change caption
        composeTestRule.onNodeWithText("Fällt aus").assertIsDisplayed()

        // Click and verify callback
        composeTestRule.onNodeWithText("Mathematik I").performClick()
        assertTrue(clickCalled)
    }
}
