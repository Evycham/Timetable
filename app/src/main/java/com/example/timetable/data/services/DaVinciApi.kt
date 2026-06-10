package com.example.timetable.data.services

import com.example.timetable.data.datenmodell.DaVinciResponse
import org.json.JSONObject
import java.io.File
import java.net.URL

data class UpdateCheckResult(
    val hasUpdates: Boolean,
    val oldFileSize: Long?,
    val newFileSize: Long,
    val cacheFile: File,
    val response: DaVinciResponse
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
    fun download(): DaVinciResponse {
        return parseResponse(fetchJson())
    }

    /**
     * Lädt die aktuellen DaVinci-Daten herunter und speichert die Raw-JSON lokal.
     *
     * @param storageDir Verzeichnis für die lokale Cache-Datei.
     * @return Die geparste API-Antwort.
     */
    fun downloadAndSave(storageDir: File): DaVinciResponse {
        val jsonString = fetchJson()
        saveRawJson(storageDir, jsonString)
        return parseResponse(jsonString)
    }

    /**
     * Laedt die lokal gespeicherte Raw-JSON-Datei und parsed sie.
     *
     * @param storageDir Verzeichnis der lokalen Cache-Datei.
     * @return Die geparste API-Antwort oder `null`, wenn noch keine Datei existiert.
     */
    fun loadFromCache(storageDir: File): DaVinciResponse? {
        val cacheFile = storageDir.resolve(RAW_CACHE_FILE_NAME)
        if (!cacheFile.exists()) return null

        return parseResponse(cacheFile.readText())
    }

    /**
     * Prüft, ob sich die DaVinci-Daten geändert haben, und ersetzt bei Bedarf die alte Cache-Datei.
     *
     * @param storageDir Verzeichnis für die lokale Cache-Datei.
     * @return Ergebnis mit Update-Status, Dateigrößen und der aktuellen API-Antwort.
     */
    fun checkForUpdates(storageDir: File): UpdateCheckResult {
        val cacheFile = storageDir.resolve(RAW_CACHE_FILE_NAME)
        val oldJson = cacheFile.takeIf { it.exists() }?.readText()
        val newJson = fetchJson()

        val oldFileSize = oldJson?.toByteArray(Charsets.UTF_8)?.size?.toLong()
        val newFileSize = newJson.toByteArray(Charsets.UTF_8).size.toLong()
        val hasUpdates = oldJson == null || oldFileSize != newFileSize || oldJson != newJson

        if (hasUpdates) {
            saveRawJson(storageDir, newJson)
        }

        val responseJson = if (hasUpdates) newJson else oldJson ?: newJson

        return UpdateCheckResult(
            hasUpdates = hasUpdates,
            oldFileSize = oldFileSize,
            newFileSize = newFileSize,
            cacheFile = cacheFile,
            response = parseResponse(responseJson)
        )
    }

    private fun fetchJson(): String =
        downloader?.invoke() ?: URL(url).readText()

    private fun saveRawJson(storageDir: File, jsonString: String): File {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val cacheFile = storageDir.resolve(RAW_CACHE_FILE_NAME)
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
        cacheFile.writeText(jsonString)

        return cacheFile
    }

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

    companion object {
        private const val DEFAULT_URL =
            "https://infoserver.hochschule-stralsund.de/daVinciIS.dll?content=json"
        const val RAW_CACHE_FILE_NAME = "davinci_raw.json"
    }
}
