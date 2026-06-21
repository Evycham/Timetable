package com.example.timetable.view.components.timetable

import com.example.timetable.data.model.Lesson
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class TimetableGridTest {

    private fun createLesson(startTime: String, endTime: String, title: String = "Test-Course"): Lesson {
        return Lesson(
            id = UUID.randomUUID().toString(),
            title = title,
            date = "2026-06-21",
            startTime = startTime,
            endTime = endTime,
            groupsCode = emptySet()
        )
    }

    @Test
    fun testEmptyLessons() {
        val groups = groupOverlappingLessons(emptyList())
        assertEquals(0, groups.size)
    }

    @Test
    fun testNoOverlap() {
        val l1 = createLesson("08:00", "09:30")
        val l2 = createLesson("09:45", "11:15")
        val l3 = createLesson("11:30", "13:00")

        val groups = groupOverlappingLessons(listOf(l1, l2, l3))
        assertEquals(3, groups.size)
        assertEquals(1, groups[0].size)
        assertEquals(l1, groups[0][0])
        assertEquals(l2, groups[1][0])
        assertEquals(l3, groups[2][0])
    }

    @Test
    fun testSimpleOverlap() {
        val l1 = createLesson("08:00", "09:30")
        val l2 = createLesson("09:00", "10:30") // overlaps l1

        val groups = groupOverlappingLessons(listOf(l1, l2))
        assertEquals(1, groups.size)
        assertEquals(2, groups[0].size)
        assertEquals(l1, groups[0][0])
        assertEquals(l2, groups[0][1])
    }

    @Test
    fun testTransitiveOverlap() {
        val l1 = createLesson("08:00", "09:30")
        val l2 = createLesson("09:00", "10:30") // overlaps l1
        val l3 = createLesson("10:00", "11:30") // overlaps l2 (max end of group is 10:30)

        val groups = groupOverlappingLessons(listOf(l1, l2, l3))
        assertEquals(1, groups.size)
        assertEquals(3, groups[0].size)
        assertEquals(l1, groups[0][0])
        assertEquals(l2, groups[0][1])
        assertEquals(l3, groups[0][2])
    }

    @Test
    fun testMultipleSeparateGroups() {
        val l1 = createLesson("08:00", "09:30")
        val l2 = createLesson("09:00", "10:30") // overlaps l1
        val l3 = createLesson("11:00", "12:30") // no overlap
        val l4 = createLesson("12:00", "13:30") // overlaps l3

        val groups = groupOverlappingLessons(listOf(l1, l2, l3, l4))
        assertEquals(2, groups.size)
        assertEquals(2, groups[0].size)
        assertEquals(2, groups[1].size)
        assertEquals(l1, groups[0][0])
        assertEquals(l2, groups[0][1])
        assertEquals(l3, groups[1][0])
        assertEquals(l4, groups[1][1])
    }

    @Test
    fun testInvalidTimesFallback() {
        val l1 = createLesson("invalid", "times")
        val l2 = createLesson("08:00", "09:30")

        val groups = groupOverlappingLessons(listOf(l1, l2))
        assertEquals(2, groups.size)
    }
}
