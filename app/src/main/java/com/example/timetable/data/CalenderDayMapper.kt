package com.example.timetable.data

import com.example.timetable.data.datenmodell.CalenderDay
import com.example.timetable.data.datenmodell.Event
import com.example.timetable.data.datenmodell.Lesson
import java.time.LocalDate

object CalenderDayMapper {

    /**
     * Baut aus Lessons und Events eine sortierte Tagesliste fuer den Kalender.
     *
     * @param lessons Bereits geparste Lessons.
     * @param events Bereits geparste Events.
     * @return Nach Datum sortierte Kalendertage mit Lessons und Events.
     */
    fun build(
        lessons: List<Lesson>,
        events: List<Event>
    ): List<CalenderDay> {
        val lessonsByDate = lessons
            .groupBy { it.date }
            .mapValues { (_, dayLessons) -> dayLessons.sortedBy { it.startTime } }

        val eventsByDate = buildEventsByDate(events)
        val allDates = (lessonsByDate.keys + eventsByDate.keys).sorted()

        return allDates.map { date ->
            CalenderDay(
                date = date,
                lessons = lessonsByDate[date].orEmpty(),
                events = eventsByDate[date].orEmpty()
            )
        }
    }

    private fun buildEventsByDate(events: List<Event>): Map<String, List<Event>> {
        if (events.isEmpty()) return emptyMap()

        val eventsByDate = mutableMapOf<String, MutableList<Event>>()

        for (event in events) {
            val dates = expandEventDates(event)
            for (date in dates) {
                eventsByDate.getOrPut(date) { mutableListOf() }.add(event)
            }
        }

        return eventsByDate
    }

    private fun expandEventDates(event: Event): List<String> {
        return try {
            val start = LocalDate.parse(event.startDate)
            val end = LocalDate.parse(event.endDate)

            if (end.isBefore(start)) {
                listOf(event.startDate)
            } else {
                generateSequence(start) { current ->
                    current.plusDays(1).takeIf { !it.isAfter(end) }
                }.map(LocalDate::toString).toList()
            }
        } catch (_: Exception) {
            listOf(event.startDate)
        }
    }
}
