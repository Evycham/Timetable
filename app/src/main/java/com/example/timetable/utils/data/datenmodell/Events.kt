package com.example.timetable.utils.data.datenmodell

data class Event(
    val id: String? = null,
    val title: String,
    val startDate: String,
    val endDate: String,
    val category: String? = null,
)


val localHolidayEvents = listOf(
    Event(
        id = "event-001",
        title = "Sommerferien",
        startDate = "2026-07-20",
        endDate = "2026-08-31",
        category = "Ferien",
    ),
    Event(
        id = "event-002",
        title = "Tag der Deutschen Einheit",
        startDate = "2026-10-03",
        endDate = "2026-10-03",
        category = "Feiertag",
    )
)