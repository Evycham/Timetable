package com.example.timetable.utils.data.datenmodell

import org.json.JSONArray

data class DaVinciResponse(
    val lessonTimes: JSONArray,
    val eventTimes: JSONArray
)