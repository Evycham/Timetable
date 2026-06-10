package com.example.timetable.data

import org.json.JSONArray

data class DaVinciResponse(
    val lessonTimes: JSONArray,
    val eventTimes: JSONArray
)