package com.example.timetable

import com.example.timetable.data.DaVinciApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class DaVinciApiLocalTest {

    @Test
    fun downloadAndSave_writesRawJsonToCacheFile() {
        val tempDir = Files.createTempDirectory("davinci-api-test").toFile()
        val api = DaVinciApi(downloader = { sampleJson(lessonCount = 2, eventCount = 1) })

        val response = api.downloadAndSave(tempDir)
        val cacheFile = tempDir.resolve(DaVinciApi.RAW_CACHE_FILE_NAME)

        assertTrue(cacheFile.exists())
        assertTrue(cacheFile.readText().isNotBlank())
        assertEquals(2, response.lessonTimes.length())
        assertEquals(1, response.eventTimes.length())
    }

    @Test
    fun checkForUpdates_returnsFalse_whenRawJsonIsUnchanged() {
        val tempDir = Files.createTempDirectory("davinci-api-test").toFile()
        val json = sampleJson(lessonCount = 1, eventCount = 1)
        val api = DaVinciApi(downloader = { json })

        api.downloadAndSave(tempDir)
        val result = api.checkForUpdates(tempDir)

        assertFalse(result.hasUpdates)
        assertEquals(result.oldFileSize, result.newFileSize)
        assertEquals(1, result.response.lessonTimes.length())
        assertEquals(1, result.response.eventTimes.length())
    }

    @Test
    fun checkForUpdates_overwritesCache_whenJsonChanged() {
        val tempDir = Files.createTempDirectory("davinci-api-test").toFile()
        val firstJson = sampleJson(lessonCount = 1, eventCount = 1)
        val secondJson = sampleJson(lessonCount = 3, eventCount = 2)

        DaVinciApi(downloader = { firstJson }).downloadAndSave(tempDir)
        val result = DaVinciApi(downloader = { secondJson }).checkForUpdates(tempDir)

        assertTrue(result.hasUpdates)
        assertTrue(result.cacheFile.exists())
        assertEquals(3, result.response.lessonTimes.length())
        assertEquals(2, result.response.eventTimes.length())
        assertEquals(secondJson, result.cacheFile.readText())
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
