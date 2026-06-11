package com.example.timetable.data.services

import com.example.timetable.data.datenmodell.DaVinciResponse
import org.json.JSONObject
import java.net.URL
import java.security.MessageDigest

data class DaVinciDownloadSnapshot(
    val rawJson: String,
    val response: DaVinciResponse,
    val jsonSize: Long,
    val jsonHash: String
)

class DaVinciApi(
    private val url: String = DEFAULT_URL,
    private val downloader: (() -> String)? = null
) {

    /**
     * Lädt die aktuellen DaVinci-Daten aus dem Netz und parsed sie direkt.
     *
     * @return Die geparste API-Antwort.
     */
    fun download(): DaVinciResponse =
        downloadSnapshot().response

    /**
     * Lädt die aktuellen DaVinci-Daten aus dem Netz und liefert zusätzlich Hash und Größe.
     *
     * @return Snapshot mit Raw-JSON, geparster Antwort und Vergleichsmetadaten.
     */
    fun downloadSnapshot(): DaVinciDownloadSnapshot {
        val jsonString = fetchJson()
        return DaVinciDownloadSnapshot(
            rawJson = jsonString,
            response = parseResponse(jsonString),
            jsonSize = jsonString.toByteArray(Charsets.UTF_8).size.toLong(),
            jsonHash = sha256(jsonString)
        )
    }

    private fun fetchJson(): String =
        downloader?.invoke() ?: URL(url).readText()

    private fun parseResponse(jsonString: String): DaVinciResponse {
        val root = JSONObject(jsonString)
        val result = root.getJSONObject("result")
        val displaySchedule = result.getJSONObject("displaySchedule")

        val lessonTimes = displaySchedule.optJSONArray("lessonTimes")
            ?: throw IllegalStateException("lessonTimes fehlen in der DaVinci-Antwort")
        val eventTimes = displaySchedule.optJSONArray("eventTimes")
            ?: throw IllegalStateException("eventTimes fehlen in der DaVinci-Antwort")

        return DaVinciResponse(
            lessonTimes = lessonTimes,
            eventTimes = eventTimes
        )
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }

    companion object {
        private const val DEFAULT_URL =
            "https://infoserver.hochschule-stralsund.de/daVinciIS.dll?content=json"
    }
}
