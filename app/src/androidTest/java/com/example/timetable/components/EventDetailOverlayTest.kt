package com.example.timetable.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.timetable.data.datenmodell.Lesson
import com.example.timetable.view.components.timetable.EventDetailOverlay
import com.example.timetable.view.theme.TimeTableTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EventDetailOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun eventDetailOverlay_whenLessonIsNull_doesNotShow() {
        composeTestRule.setContent {
            TimeTableTheme {
                EventDetailOverlay(
                    lesson = null,
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Vorlesungsdetails").assertDoesNotExist()
    }

    @Test
    fun eventDetailOverlay_whenLessonIsNotNull_showsDetails() {
        val testLesson = Lesson(
            id = "lesson-123",
            title = "Algorithmen & Datenstrukturen",
            date = "2026-06-15",
            startTime = "08:00",
            endTime = "09:30",
            rooms = setOf("4/201"),
            building = "H4",
            teacher = setOf("Prof. Dr. Schmidt"),
            groupsCode = setOf("eti-INF_2"),
            change = Lesson.Change(
                caption = "Raumänderung",
                reasonType = "roomChange",
                modified = "2026-06-10"
            )
        )

        var dismissCalled = false

        composeTestRule.setContent {
            TimeTableTheme {
                EventDetailOverlay(
                    lesson = testLesson,
                    onDismiss = { dismissCalled = true }
                )
            }
        }

        // Verify title and headers
        composeTestRule.onNodeWithText("Vorlesungsdetails").assertIsDisplayed()
        composeTestRule.onNodeWithText("Algorithmen & Datenstrukturen").assertIsDisplayed()

        // Verify lesson details
        composeTestRule.onNodeWithText("08:00 - 09:30").assertIsDisplayed()
        composeTestRule.onNodeWithText("4/201 (H4)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prof. Dr. Schmidt").assertIsDisplayed()

        // Verify change banner is displayed
        composeTestRule.onNodeWithText("Raumänderung").assertIsDisplayed()

        // Click close and check callback
        composeTestRule.onNodeWithContentDescription("Schließen").performClick()
        assertTrue(dismissCalled)
    }
}
