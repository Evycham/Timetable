package com.example.timetable.data.local.db.mapper

import com.example.timetable.data.local.db.entities.EventEntity
import com.example.timetable.data.local.db.entities.LessonWithRelations
import com.example.timetable.data.model.Event
import com.example.timetable.data.model.Lesson

fun LessonWithRelations.toModel() = Lesson(
    id = lesson.id,
    title = lesson.title,
    date = lesson.date,
    startTime = lesson.startTime,
    endTime = lesson.endTime,
    rooms = rooms.toSet().takeIf { it.isNotEmpty() },
    building = lesson.building,
    teacher = teachers.toSet().takeIf { it.isNotEmpty() },
    groupsCode = groups.toSet(),
    change = if (
        lesson.changeCaption != null ||
        lesson.changeReasonType != null ||
        lesson.changeModified != null
    ) {
        Lesson.Change(
            caption = lesson.changeCaption,
            reasonType = lesson.changeReasonType,
            modified = lesson.changeModified
        )
    } else null
)

fun EventEntity.toModel() = Event(
    id = id,
    title = title,
    startDate = startDate,
    endDate = endDate,
    category = category
)

fun Event.toEntity() = EventEntity(
    id = id ?: "",
    title = title,
    startDate = startDate,
    endDate = endDate,
    category = category
)
