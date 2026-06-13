package com.example.timetable

import com.example.timetable.data.remote.DaVinciApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DaVinciApiLocalTest {

    @Test
    fun download_returnsParsedResponse() = runBlocking {
        val json = """{"result":{"displaySchedule":{"lessonTimes":[], "eventTimes":[]}}}"""
        val api = DaVinciApi(downloader = { json })

        val response = api.download()

        assertEquals(0, response.lessonTimes.length())
        assertEquals(0, response.eventTimes.length())
    }

    @Test
    fun downloadSnapshot_returnsStableHash_forSameJson() = runBlocking {
        val json = """{"result":{"displaySchedule":{"lessonTimes":[], "eventTimes":[]}}}"""
        val api = DaVinciApi(downloader = { json })

        val snapshot1 = api.downloadSnapshot()
        val snapshot2 = api.downloadSnapshot()

        assertEquals(snapshot1.jsonHash, snapshot2.jsonHash)
        assertEquals(json.length.toLong(), snapshot1.jsonSize)
    }

    @Test
    fun downloadSnapshot_returnsCorrectSize_forLargeJson() = runBlocking {
        val json = buildString {
            append("""{"result":{"displaySchedule":{"lessonTimes":[""")
            repeat(100) { index ->
                if (index > 0) append(",")
                append("""{"courseTitle":"Lesson $index","dates":["2026051$index"]}""")
            }
            append("""], "eventTimes":[]}}}""")
        }
        val api = DaVinciApi(downloader = { json })

        val snapshot = api.downloadSnapshot()

        assertEquals(json.length.toLong(), snapshot.jsonSize)
    }
}
