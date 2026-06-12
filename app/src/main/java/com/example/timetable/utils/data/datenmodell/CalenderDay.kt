package com.example.timetable.utils.data.datenmodell

data class CalenderDay(
    val date: String,
    val lessons: List<Lesson>,
    val events: List<Event>
)
