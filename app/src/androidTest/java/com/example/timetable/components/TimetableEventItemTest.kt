package com.example.timetable.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.timetable.utils.data.datenmodell.Event
import com.example.timetable.view.components.timetable.TimeTableEventItem
import com.example.timetable.view.theme.TimeTableTheme
import org.junit.Rule
import org.junit.Test

class TimetableEventItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun timetableEventItem_displaysTitleAndCategory() {
        val testEvent = _root_ide_package_.com.example.timetable.utils.data.datenmodell.Event(
            id = "test-event-1",
            title = "Weihnachtsferien",
            startDate = "2026-12-21",
            endDate = "2027-01-03",
            category = "Ferien"
        )

        composeTestRule.setContent {
            TimeTableTheme {
                TimeTableEventItem(event = testEvent)
            }
        }

        // Verify title is displayed
        composeTestRule.onNodeWithText("Weihnachtsferien").assertIsDisplayed()

        // Verify category is displayed in uppercase
        composeTestRule.onNodeWithText("FERIEN").assertIsDisplayed()
    }
}
