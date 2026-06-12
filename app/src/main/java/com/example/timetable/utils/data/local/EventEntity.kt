package com.example.timetable.utils.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val category: String?
)
