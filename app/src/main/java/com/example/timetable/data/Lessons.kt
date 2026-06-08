package com.example.timetable.data

data class TimetableData(
    val lessons: List<Lesson>,
    val holidayEvents: List<HolidayEvent>
)

data class Lesson(
    val serverId: String?,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val room: String?,
    val building: String?,
    val className: String?,
    val changes: Change?
) {
    data class Change(
        val caption: String?,
        val reasonType: String?,
        val modified: String?
    )
}

data class HolidayEvent(
    val serverId: String?,
    val title: String,
    val startDate: String,
    val endDate: String,
    val wholeDay: Boolean
)
