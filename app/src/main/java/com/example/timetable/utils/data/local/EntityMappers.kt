package com.example.timetable.utils.data.local

import com.example.timetable.utils.data.datenmodell.Event
import com.example.timetable.utils.data.datenmodell.Lesson

fun Lesson.toEntity(): LessonEntity = LessonEntity(
    id = id,
    title = title,
    date = date,
    startTime = startTime,
    endTime = endTime,
    rooms = rooms,
    building = building,
    teacher = teacher,
    groupsCode = groupsCode,
    changeCaption = change?.caption,
    changeReasonType = change?.reasonType,
    changeModified = change?.modified
)

fun LessonEntity.toModel(): Lesson = Lesson(
    id = id,
    title = title,
    date = date,
    startTime = startTime,
    endTime = endTime,
    rooms = rooms,
    building = building,
    teacher = teacher,
    groupsCode = groupsCode,
    change = if (changeCaption == null && changeReasonType == null && changeModified == null) {
        null
    } else {
        Lesson.Change(
            caption = changeCaption,
            reasonType = changeReasonType,
            modified = changeModified
        )
    }
)

fun Event.toEntity(): EventEntity = EventEntity(
    id = id ?: stableEventId(),
    title = title,
    startDate = startDate,
    endDate = endDate,
    category = category
)

fun EventEntity.toModel(): Event = Event(
    id = id,
    title = title,
    startDate = startDate,
    endDate = endDate,
    category = category
)

private fun Event.stableEventId(): String =
    listOf(title, startDate, endDate, category.orEmpty()).joinToString("#")
