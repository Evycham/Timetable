package com.example.timetable.view.json

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JsonLessonRepositoryTest {

    private lateinit var repository: JsonLessonRepository
    private val testJson = """
        {
          "faculties": [
            {
              "code": "eti",
              "name": "Elektrotechnik und Informationstechnik",
              "color": "#0000FF"
            },
            {
              "code": "mb",
              "name": "Maschinenbau",
              "color": "#008000"
            }
          ],
          "lessons": [
            {
              "id": "lesson_1",
              "title": "eti-Autonome Mobile Systeme Vorl.",
              "date": "2026-06-15",
              "startTime": "10:15",
              "endTime": "11:45",
              "room": "4/221 (H4)",
              "lecturer": "eti-Garske",
              "course": "eti-Auton. Mob. Syst. Vorl.",
              "change": {
                "caption": "Zusatztermin",
                "message": "Wichtiger Zusatztermin für Klausurvorbereitung."
              }
            },
            {
              "id": "lesson_2",
              "title": "mb-Kostenrechnung und Kostenanalyse - ÜB",
              "date": "2026-06-16",
              "startTime": "09:45",
              "endTime": "11:15",
              "room": "4/206 (H4)",
              "lecturer": "mb-Türr",
              "course": "mb-SPB_4, eti-WETB 4, mb-WIB_4, mb-GTMB_4",
              "change": null
            }
          ]
        }
    """.trimIndent()

    @Before
    fun setUp() {
        repository = JsonLessonRepository(null)
        repository.parseJson(testJson)
    }

    @Test
    fun `getFaculties returns correct list`() {
        val faculties = repository.getFaculties()
        assertEquals(2, faculties.size)
        assertEquals("eti", faculties[0].code)
        assertEquals("mb", faculties[1].code)
    }

    @Test
    fun `getAllCourses extracts and splits courses correctly`() {
        val courses = repository.getAllCourses()
        // eti-Auton. Mob. Syst. Vorl.
        // mb-SPB_4, eti-WETB 4, mb-WIB_4, mb-GTMB_4
        assertEquals(5, courses.size)
        assertTrue(courses.contains("eti-Auton. Mob. Syst. Vorl."))
        assertTrue(courses.contains("mb-SPB_4"))
        assertTrue(courses.contains("eti-WETB 4"))
    }

    @Test
    fun `getCoursesByFaculty filters correctly`() {
        val etiCourses = repository.getCoursesByFaculty("eti-")
        assertEquals(2, etiCourses.size)
        assertTrue(etiCourses.contains("eti-Auton. Mob. Syst. Vorl."))
        assertTrue(etiCourses.contains("eti-WETB 4"))

        val mbCourses = repository.getCoursesByFaculty("mb-")
        assertEquals(3, mbCourses.size)
        assertTrue(mbCourses.contains("mb-SPB_4"))
    }

    @Test
    fun `searchModules filters by professor`() {
        val results = repository.searchModules("Garske")
        assertEquals(1, results.size)
        assertEquals("eti-Autonome Mobile Systeme Vorl.", results[0].title)
    }

    @Test
    fun `searchModules filters by room`() {
        val results = repository.searchModules("4/206")
        assertEquals(1, results.size)
        assertEquals("mb-Kostenrechnung und Kostenanalyse - ÜB", results[0].title)
    }
}
