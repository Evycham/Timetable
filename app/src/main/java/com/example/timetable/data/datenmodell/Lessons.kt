package com.example.timetable.data.datenmodell

import java.util.UUID

data class Lesson(
    val id: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val rooms: Set<String>?,
    val building: String?,
    val teacher: Set<String>?,
    val groupsCode: Set<String>,
    val change: Change?
) {
    data class Change(
        val caption: String?,
        val reasonType: String?,
        val modified: String?
    )
}

/**
 * Testdaten
 * */
object LessonSamples {
    val localTestLessons = listOf(
        Lesson(
            id = UUID.randomUUID().toString(),
            title = "mb-Kostenrechnung und Kostenanalyse - ÜB",
            date = "2026-05-20",
            startTime = "09:45",
            endTime = "11:15",
            rooms = setOf(
                "4/206"
            ),
            building = "H4",
            teacher = setOf(
                "mb-Türr"
            ),
            groupsCode = setOf(
                "mb-SPB_4",
                "eti-WETB 4",
                "mb-WIB_4",
                "mb-GTMB_4"
            ),
            change = null
        ),
        Lesson(
            id = UUID.randomUUID().toString(),
            title = "eti-Inf.u.Gesellsch.Vorl.",
            date = "2026-05-18",
            startTime = "17:30",
            endTime = "19:00",
            rooms = setOf(
                "5/HS1"
            ),
            building = "H5",
            teacher = setOf(
                "eti-Friedenberg"
            ),
            groupsCode = setOf(
                "eti-SKIB 4",
            ),
            change = null
        ),
        Lesson(
            id = UUID.randomUUID().toString(),
            title = "eti-PT II Lab",
            date = "2026-05-18",
            startTime = "09:45",
            endTime = "11:15",
            rooms = setOf(
                "4/302"
            ),
            building = "H4",
            teacher = null,
            groupsCode = setOf(
                "eti-SMSB 2",
                "eti-ETM"
            ),
            change = Lesson.Change(
                caption = "Keine Vertretung",
                reasonType = "teacherAbsence",
                modified = "2026-04-07"
            )
        )
    )
}
