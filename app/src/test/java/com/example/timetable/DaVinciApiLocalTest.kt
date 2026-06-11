package com.example.timetable

import com.example.timetable.data.services.DaVinciApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DaVinciApiLocalTest {

    @Test
    fun downloadSnapshot_returnsParsedResponseWithHashAndSize() {
        val json = sampleJson(lessonCount = 2, eventCount = 1)
        val api = DaVinciApi(downloader = { json })

        val snapshot = api.downloadSnapshot()

        assertEquals(json, snapshot.rawJson)
        assertEquals(json.toByteArray(Charsets.UTF_8).size.toLong(), snapshot.jsonSize)
        assertTrue(snapshot.jsonHash.isNotBlank())
        assertEquals(2, snapshot.response.lessonTimes.length())
        assertEquals(1, snapshot.response.eventTimes.length())
    }

    @Test
    fun downloadSnapshot_returnsStableHash_forSameJson() {
        val json = sampleJson(lessonCount = 1, eventCount = 1)

        val first = DaVinciApi(downloader = { json }).downloadSnapshot()
        val second = DaVinciApi(downloader = { json }).downloadSnapshot()

        assertEquals(first.jsonHash, second.jsonHash)
        assertEquals(first.jsonSize, second.jsonSize)
    }

    @Test
    fun downloadSnapshot_returnsDifferentHash_forChangedJson() {
        val firstJson = sampleJson(lessonCount = 1, eventCount = 1)
        val secondJson = sampleJson(lessonCount = 3, eventCount = 2)

        val first = DaVinciApi(downloader = { firstJson }).downloadSnapshot()
        val second = DaVinciApi(downloader = { secondJson }).downloadSnapshot()

        assertNotEquals(first.jsonHash, second.jsonHash)
        assertNotEquals(first.jsonSize, second.jsonSize)
        assertEquals(3, second.response.lessonTimes.length())
        assertEquals(2, second.response.eventTimes.length())
    }

    private fun sampleJson(lessonCount: Int, eventCount: Int): String {
        val lessons = buildString {
            append("[")
            repeat(lessonCount) { index ->
                if (index > 0) append(",")
                append("""{"courseTitle":"Lesson $index","dates":["2026051${index}"]}""")
            }
            append("]")
        }

        val events = buildString {
            append("[")
            repeat(eventCount) { index ->
                if (index > 0) append(",")
                append("""{"id":"event-$index","title":"Event $index","startDate":"2026061${index}","endDate":"2026061${index}"}""")
            }
            append("]")
        }

        return """
            {
              "result": {
                "displaySchedule": {
                  "lessonTimes": $lessons,
                  "eventTimes": $events
                }
              }
            }
        """.trimIndent()
    }
}
