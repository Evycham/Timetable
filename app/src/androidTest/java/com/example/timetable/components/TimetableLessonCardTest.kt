package com.example.timetable.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.graphics.Color
import com.example.timetable.view.components.timetable.TimetableLessonCard
import com.example.timetable.data.model.Lesson
import com.example.timetable.view.theme.TimeTableTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TimetableLessonCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun timetableLessonCard_displaysAllDetailsAndHandlesClick() {
        val testLesson = Lesson(
            id = "lesson-123",
            title = "Mathematik I",
            date = "2026-06-15",
            startTime = "09:45",
            endTime = "11:15",
            rooms = setOf("4/206 (H4)"),
            teacher = setOf("Dr. Müller"),
            groupsCode = setOf("eti-INF_2"),
            change = Lesson.Change(
                caption = "Fällt aus",
                reasonType = "cancellation",
                modified = "2026-06-15"
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
