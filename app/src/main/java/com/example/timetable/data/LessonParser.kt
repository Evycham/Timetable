package com.example.timetable.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LessonParser {

    /**
     * Parsed ein komplettes Lesson-Array aus der API in eine flache Liste von Lessons.
     *
     * @param array JSON-Array aus `lessonTimes`.
     * @return Liste aller erzeugten Lessons, bei `null` eine leere Liste.
     */
    fun parseLessons(array: JSONArray?): List<Lesson> {
        if (array == null) return emptyList()

        val lessons = mutableListOf<Lesson>()
        for (i in 0 until array.length()) {
            val lessonObject = array.optJSONObject(i) ?: continue
            lessons += parseLesson(lessonObject)
        }
        return lessons
    }

    /**
     * Parsed einen einzelnen API-Lesson-Eintrag in ein oder mehrere Lessons anhand der enthaltenen Daten.
     *
     * @param obj Ein JSON-Objekt aus `lessonTimes`.
     * @return Ein Set von Lessons, bei ungültigen Pflichtfeldern ein leeres Set.
     */
    fun parseLesson(obj: JSONObject): Set<Lesson> {
        val title = obj.optString("courseTitle").takeIf { it.isNotBlank() } ?: return emptySet()
        val dates = jsonArrayToList(obj.optJSONArray("dates")).map(::formatDate)
        if (dates.isEmpty()) return emptySet()

        val startTime = formatTime(obj.optString("startTime"))
        val endTime = formatTime(obj.optString("endTime"))
        val rooms = jsonArrayToSet(obj.optJSONArray("roomCodes"))
        val teachers = jsonArrayToSet(obj.optJSONArray("teacherCodes"))
        val groupsCode = jsonArrayToSet(obj.optJSONArray("classCodes"))
        val building = jsonArrayToList(obj.optJSONArray("buildingCodes")).firstOrNull()
        val change = parseChange(obj)

        val lessons = linkedSetOf<Lesson>()
        for (date in dates) {
            lessons.add(
                Lesson(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    rooms = rooms.takeIf { it.isNotEmpty() },
                    building = building,
                    teacher = teachers.takeIf { it.isNotEmpty() },
                    groupsCode = groupsCode,
                    change = change
                )
            )
        }
        return lessons
    }

    /**
     * Parsed ein komplettes Event-Array aus der API in eine Liste von Holiday-Events.
     *
     * @param array JSON-Array aus `eventTimes`.
     * @return Liste aller gültigen Holiday-Events, bei `null` eine leere Liste.
     */
    fun parseEvents(array: JSONArray?): List<HolidayEvent> {
        if (array == null) return emptyList()

        val events = mutableListOf<HolidayEvent>()
        for (i in 0 until array.length()) {
            val eventObject = array.optJSONObject(i) ?: continue
            val event = parseEvent(eventObject) ?: continue
            events += event
        }
        return events
    }

    /**
     * Parsed ein einzelnes Event-Objekt aus der API.
     *
     * @param obj Ein JSON-Objekt aus `eventTimes`.
     * @return Ein HolidayEvent oder `null`, falls Pflichtfelder fehlen.
     */
    fun parseEvent(obj: JSONObject): HolidayEvent? {
        val title = obj.optString("title").takeIf { it.isNotBlank() } ?: return null
        val startDate = formatDate(obj.optString("startDate")).ifBlank { return null }
        val endDate = formatDate(obj.optString("endDate")).ifBlank { startDate }
        val category = obj.optString("category").takeIf { it.isNotBlank() }

        return HolidayEvent(
            id = obj.optString("id").takeIf { it.isNotBlank() },
            title = title,
            startDate = startDate,
            endDate = endDate,
            category = category
        )
    }

    /**
     * Parsed optionale Change-Informationen eines Lessons.
     *
     * @param obj Ein JSON-Objekt, das optional ein `change`-Objekt enthält.
     * @return Ein Change-Objekt oder `null`, wenn keine Änderungsdaten vorhanden sind.
     */
    fun parseChange(obj: JSONObject): Lesson.Change? {
        val source = obj.optJSONObject("change") ?: return null
        val caption = source.optString("caption").takeIf { it.isNotBlank() }
        val reasonType = source.optString("reasonType").takeIf { it.isNotBlank() }
        val modified = source.optString("modified")
            .takeIf { it.isNotBlank() }
            ?.let(::formatDate)

        if (caption == null && reasonType == null && modified == null) {
            return null
        }

        return Lesson.Change(
            caption = caption,
            reasonType = reasonType,
            modified = modified
        )
    }

    /**
     * Wandelt ein JSON-Array in ein Set von nicht-leeren Strings um.
     *
     * @param array Das zu lesende JSON-Array.
     * @return Ein Set mit allen nicht-leeren Einträgen.
     */
    private fun jsonArrayToSet(array: JSONArray?): Set<String> =
        jsonArrayToList(array).toSet()

    /**
     * Wandelt ein JSON-Array in eine Liste von nicht-leeren Strings um.
     *
     * @param array Das zu lesende JSON-Array.
     * @return Eine Liste mit allen nicht-leeren Einträgen.
     */
    private fun jsonArrayToList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()

        val result = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val value = array.optString(i).trim()
            if (value.isNotEmpty()) {
                result += value
            }
        }
        return result
    }

    /**
     * Formatiert API-Datumswerte wie `20260518` in `2026-05-18`.
     *
     * @param raw Der rohe Datumswert aus der API.
     * @return Das formatierte Datum oder der Originalwert, falls kein Umbau passt.
     */
    private fun formatDate(raw: String): String {
        val value = raw.trim()
        if (value.length == 8 && value.all(Char::isDigit)) {
            return "${value.substring(0, 4)}-${value.substring(4, 6)}-${value.substring(6, 8)}"
        }
        return value
    }

    /**
     * Formatiert API-Zeitwerte wie `1400` in `14:00`.
     *
     * @param raw Der rohe Zeitwert aus der API.
     * @return Die formatierte Uhrzeit oder der Originalwert, falls kein Umbau passt.
     */
    private fun formatTime(raw: String): String {
        val value = raw.trim()
        if (value.length == 4 && value.all(Char::isDigit)) {
            return "${value.substring(0, 2)}:${value.substring(2, 4)}"
        }
        return value
    }
}
