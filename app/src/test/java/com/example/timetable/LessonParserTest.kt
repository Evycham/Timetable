package com.example.timetable

import com.example.timetable.data.datenmodell.Lesson
import com.example.timetable.data.services.LessonParser
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonParserTest {

    private val parser = LessonParser()

    @Test
    fun parseLesson_createsOneLessonPerDate_andFormatsFields() {
        val lessonJson = JSONObject(
            """
            {
              "courseTitle": "mb-Kinematik, Kinetik, Maschinendynamik - Lab",
              "dates": ["20260518", "20260608"],
              "startTime": "1400",
              "endTime": "1530",
              "classCodes": ["mb-MBB_4", "mb-MSEB_4"],
              "teacherCodes": ["mb-Wilmers"],
              "roomCodes": ["4/322"],
              "buildingCodes": ["H4"],
              "change": {
                "caption": "Raum geaendert",
                "reasonType": "roomChange",
                "modified": "20260510"
              }
            }
            """.trimIndent()
        )

        val lessons = parser.parseLesson(lessonJson).sortedBy { it.date }

        assertEquals(2, lessons.size)

        val firstLesson = lessons.first()
        assertEquals("mb-Kinematik, Kinetik, Maschinendynamik - Lab", firstLesson.title)
        assertEquals("2026-05-18", firstLesson.date)
        assertEquals("14:00", firstLesson.startTime)
        assertEquals("15:30", firstLesson.endTime)
        assertEquals(setOf("4/322"), firstLesson.rooms)
        assertEquals("H4", firstLesson.building)
        assertEquals(setOf("mb-Wilmers"), firstLesson.teacher)
        assertEquals(setOf("mb-MBB_4", "mb-MSEB_4"), firstLesson.groupsCode)
        assertNotNull(firstLesson.change)
        assertEquals(
            Lesson.Change(
                caption = "Raum geaendert",
                reasonType = "roomChange",
                modified = "2026-05-10"
            ),
            firstLesson.change
        )

        val secondLesson = lessons.last()
        assertEquals("2026-06-08", secondLesson.date)
    }

    @Test
    fun parseLesson_returnsEmptySet_whenRequiredFieldsAreMissing() {
        val lessonWithoutTitle = JSONObject(
            """
            {
              "dates": ["20260518"],
              "startTime": "1400",
              "endTime": "1530"
            }
            """.trimIndent()
        )

        val lessonWithoutDates = JSONObject(
            """
            {
              "courseTitle": "Mathe"
            }
            """.trimIndent()
        )

        assertTrue(parser.parseLesson(lessonWithoutTitle).isEmpty())
        assertTrue(parser.parseLesson(lessonWithoutDates).isEmpty())
    }

    @Test
    fun parseLesson_keepsOptionalCollectionsNull_whenArraysAreMissing() {
        val lessonJson = JSONObject(
            """
            {
              "courseTitle": "eti-PT II Lab",
              "dates": ["20260518"],
              "startTime": "0945",
              "endTime": "1115",
              "classCodes": ["eti-SMSB 2", "eti-ETM"]
            }
            """.trimIndent()
        )

        val lesson = parser.parseLesson(lessonJson).first()

        assertNull(lesson.rooms)
        assertNull(lesson.teacher)
        assertNull(lesson.building)
        assertEquals(setOf("eti-SMSB 2", "eti-ETM"), lesson.groupsCode)
        assertNull(lesson.change)
    }

    @Test
    fun parseLessons_flattensWholeArray() {
        val lessonsArray = JSONArray(
            """
            [
              {
                "courseTitle": "A",
                "dates": ["20260518", "20260519"],
                "startTime": "0800",
                "endTime": "0930",
                "classCodes": ["X"]
              },
              {
                "courseTitle": "B",
                "dates": ["20260520"],
                "startTime": "1000",
                "endTime": "1130",
                "classCodes": ["Y"]
              }
            ]
            """.trimIndent()
        )

        val lessons = parser.parseLessons(lessonsArray)

        assertEquals(3, lessons.size)
        assertEquals(setOf("A", "B"), lessons.map { it.title }.toSet())
    }

    @Test
    fun parseChange_returnsNull_whenNoChangeExists() {
        val lessonJson = JSONObject(
            """
            {
              "courseTitle": "Ohne Aenderung",
              "dates": ["20260518"]
            }
            """.trimIndent()
        )

        assertNull(parser.parseChange(lessonJson))
    }

    @Test
    fun parseEvent_formatsDates_andUsesStartDateAsFallback() {
        val eventJson = JSONObject(
            """
            {
              "id": "event-001",
              "title": "Sommerferien",
              "startDate": "20260720",
              "endDate": "",
              "category": "Ferien"
            }
            """.trimIndent()
        )

        val event = parser.parseEvent(eventJson)

        assertNotNull(event)
        assertEquals("event-001", event?.id)
        assertEquals("Sommerferien", event?.title)
        assertEquals("2026-07-20", event?.startDate)
        assertEquals("2026-07-20", event?.endDate)
        assertEquals("Ferien", event?.category)
    }

    @Test
    fun parseEvents_skipsInvalidEntries() {
        val eventsArray = JSONArray(
            """
            [
              {
                "id": "event-001",
                "title": "Sommerferien",
                "startDate": "20260720",
                "endDate": "20260831",
                "category": "Ferien"
              },
              {
                "id": "event-002",
                "startDate": "20261003"
              }
            ]
            """.trimIndent()
        )

        val events = parser.parseEvents(eventsArray)

        assertEquals(1, events.size)
        assertEquals("event-001", events.first().id)
        assertEquals("2026-08-31", events.first().endDate)
    }
}
