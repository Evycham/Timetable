package com.example.timetable

import com.example.timetable.utils.data.services.DaVinciApi
import org.junit.Assert.assertTrue
import org.junit.Test


class DaVinciApiTest {

    @Test
    fun download_returnsLessonsAndEventTimes(){
        val api = _root_ide_package_.com.example.timetable.utils.data.services.DaVinciApi()

        val response = api.download()

        assertTrue(response.lessonTimes.length() > 0)
        assertTrue(response.eventTimes.length() > 0)
    }
}