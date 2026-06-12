package com.example.timetable

import com.example.timetable.utils.data.services.DaVinciApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class DaVinciApiTest {

    @Test
    fun download_returnsLessonsAndEventTimes() = runBlocking {
        val api = DaVinciApi()

        val response = api.download()

        assertTrue(response.lessonTimes.length() > 0)
        assertTrue(response.eventTimes.length() > 0)
    }
}
