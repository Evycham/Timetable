package com.example.timetable.data

import org.json.JSONObject
import java.net.URL

class DaVinciApi {
    private val url =
        "https://infoserver.hochschule-stralsund.de/daVinciIS.dll?content=json"

    fun download(): DaVinciResponse {
        val jsonString = URL(url).readText()

        val root = JSONObject(jsonString)
        val result = root.getJSONObject("result")
        val displaySchedule = result.getJSONObject("displaySchedule")

        val lessonTimes = displaySchedule.getJSONArray("lessonTimes")
        val eventTimes = displaySchedule.getJSONArray("eventTimes")

        return DaVinciResponse(
            lessonTimes = lessonTimes,
            eventTimes = eventTimes
        )
    }
}
