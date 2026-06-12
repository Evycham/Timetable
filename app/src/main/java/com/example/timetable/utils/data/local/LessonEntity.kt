package com.example.timetable.utils.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val rooms: Set<String>?,
    val building: String?,
    val teacher: Set<String>?,
    val groupsCode: Set<String>,
    val changeCaption: String?,
    val changeReasonType: String?,
    val changeModified: String?
)
