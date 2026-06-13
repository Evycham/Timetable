package com.example.timetable.data.model

data class CalenderDay(
    val date: String,
    val lessons: List<Lesson>,
    val events: List<Event>
)
