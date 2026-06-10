package com.example.timetable

import com.example.timetable.data.services.CalenderDayMapper
import com.example.timetable.data.datenmodell.Event
import com.example.timetable.data.datenmodell.Lesson
import org.junit.Assert.assertEquals
import org.junit.Test

class CalenderDayMapperTest {

    @Test
    fun build_groupsLessons_sortsByTime_andSpreadsEventsAcrossDates() {
        val lessons = listOf(
            Lesson(
                id = "2",
                title = "Spaet",
                date = "2026-05-20",
                startTime = "11:15",
                endTime = "12:45",
                rooms = null,
                building = null,
                teacher = null,
                groupsCode = setOf("A"),
                change = null
            ),
            Lesson(
                id = "1",
                title = "Frueh",
                date = "2026-05-20",
                startTime = "09:45",
                endTime = "11:15",
                rooms = null,
                building = null,
                teacher = null,
                groupsCode = setOf("A"),
                change = null
            )
        )

        val events = listOf(
            Event(
                id = "event-1",
                title = "Projektwoche",
                startDate = "2026-05-19",
                endDate = "2026-05-21",
                category = "Block"
            )
        )

        val days = CalenderDayMapper.build(lessons, events)

        assertEquals(listOf("2026-05-19", "2026-05-20", "2026-05-21"), days.map { it.date })

        val lessonDay = days[1]
        assertEquals(listOf("Frueh", "Spaet"), lessonDay.lessons.map { it.title })
        assertEquals(1, lessonDay.events.size)

        assertEquals(1, days[0].events.size)
        assertEquals(1, days[2].events.size)
    }
}
