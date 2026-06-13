package com.example.timetable

import com.example.timetable.data.local.db.entities.HiddenLessonEntity
import com.example.timetable.data.model.Lesson
import com.example.timetable.data.services.AlertType
import com.example.timetable.data.services.TimetableAlertDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableAlertDetectorTest {

    private val detector = TimetableAlertDetector()

    @Test
    fun detectAlerts_detectsCancellation() {
        val lesson = Lesson(
            id = "1", title = "Mathe", date = "2026-05-18",
            startTime = "09:45", endTime = "11:15", groupsCode = setOf("A")
        )
        val oldLessons = listOf(lesson)
        val newLessons = emptyList<Lesson>()

        val alerts = detector.detectAlerts(
            oldUserLessons = oldLessons,
            newLessons = newLessons,
            userGroupsCode = "A",
            extraLessons = emptyList(),
            hiddenLessons = emptyList()
        )

        assertEquals(1, alerts.size)
        assertEquals(AlertType.CANCELLATION, alerts[0].type)
        assertEquals("Mathe", alerts[0].lessonTitle)
    }

    @Test
    fun detectAlerts_detectsRoomChange() {
        val oldLesson = Lesson(
            id = "1", title = "Mathe", date = "2026-05-18",
            startTime = "09:45", endTime = "11:15", groupsCode = setOf("A"),
            rooms = setOf("4/302")
        )
        val newLesson = oldLesson.copy(rooms = setOf("4/999"))
        
        val oldLessons = listOf(oldLesson)
        val newLessons = listOf(newLesson)

        val alerts = detector.detectAlerts(
            oldUserLessons = oldLessons,
            newLessons = newLessons,
            userGroupsCode = "A",
            extraLessons = emptyList(),
            hiddenLessons = emptyList()
        )

        assertEquals(1, alerts.size)
        assertEquals(AlertType.ROOM_CHANGE, alerts[0].type)
        assertTrue(alerts[0].detail.contains("4/302"))
        assertTrue(alerts[0].detail.contains("4/999"))
    }

    @Test
    fun detectAlerts_respectsHiddenRules_forRoomChange() {
        val oldLesson = Lesson(
            id = "1", title = "Mathe", date = "2026-05-18",
            startTime = "09:45", endTime = "11:15", groupsCode = setOf("A"),
            rooms = setOf("4/302")
        )
        val newLesson = oldLesson.copy(rooms = setOf("4/999"))
        
        val oldLessons = listOf(oldLesson)
        val newLessons = listOf(newLesson)

        // Rule to hide Mathe
        val hiddenRules = listOf(HiddenLessonEntity(title = "Mathe", groupsCode = "A"))

        val alerts = detector.detectAlerts(
            oldUserLessons = oldLessons,
            newLessons = newLessons,
            userGroupsCode = "A",
            extraLessons = emptyList(),
            hiddenLessons = hiddenRules
        )

        // It should still detect Cancellation if it's completely gone from NEW, 
        // but here it exists but is hidden for the user.
        // Actually, if it's hidden, matchingNew will be null.
        // If matchingNew is null, it won't add a ROOM_CHANGE alert.
        // BUT it might add a CANCELLATION alert because it's no longer in the user's plan.
        // However, detector logic step 2 checks if it exists in ALL newLessons, not just user lessons.
        
        assertEquals(0, alerts.size)
    }
}
