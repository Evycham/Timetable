package com.example.timetable

import com.example.timetable.data.DaVinciApi
import org.junit.Assert.assertTrue
import org.junit.Test


class DaVinciApiTest {

    @Test
    fun download_returnsLessonsAndEventTimes(){
        val api = DaVinciApi()

        val response = api.download()

        assertTrue(response.lessonTimes.length() > 0)
        assertTrue(response.eventTimes.length() > 0)
    }
}