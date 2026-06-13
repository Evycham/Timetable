package com.example.timetable.data.model

import org.json.JSONArray

data class DaVinciResponse(
    val lessonTimes: JSONArray,
    val eventTimes: JSONArray
)