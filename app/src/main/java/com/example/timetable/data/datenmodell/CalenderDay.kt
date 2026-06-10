package com.example.timetable.data.datenmodell

data class CalenderDay(
    val date: String,
    val lessons: List<Lesson>,
    val events: List<Event>
)
