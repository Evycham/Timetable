package com.example.timetable.view.json

import com.example.timetable.data.datenmodell.Lesson
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek

data class JsonFaculty(
    val code: String,
    val name: String,
    val color: String
)

data class JsonLesson(
    val id: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val lecturer: String,
    val course: String,
    val change: JsonLessonChange?
) {
    val dayOfWeek: DayOfWeek? by lazy {
        try {
            LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE).dayOfWeek
        } catch (e: Exception) {
            null
        }
    }

    fun toLesson(): Lesson {
        // Extrahiere Gebäude aus dem Raum-String, z.B. "4/221 (H4)" -> "H4"
        val buildingRegex = "\\(([^)]+)\\)".toRegex()
        val building = buildingRegex.find(room)?.groupValues?.get(1)

        return Lesson(
            id = id,
            title = title,
            date = date,
            startTime = startTime,
            endTime = endTime,
            rooms = setOf(room),
            building = building,
            teacher = setOf(lecturer),
            groupsCode = course.split(",").map { it.trim() }.toSet(),
            change = change?.let {
                val isCancellation = it.caption.contains("aus", ignoreCase = true) || 
                                     it.message.contains("aus", ignoreCase = true) ||
                                     it.caption.contains("cancell", ignoreCase = true)
                Lesson.Change(
                    caption = it.caption,
                    reasonType = if (isCancellation) "cancellation" else "roomChange",
                    modified = it.message
                )
            }
        )
    }
}

data class JsonLessonChange(
    val caption: String,
    val message: String
)

data class TimetableData(
    val faculties: List<JsonFaculty>,
    val lessons: List<JsonLesson>
)
