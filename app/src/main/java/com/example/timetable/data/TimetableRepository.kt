package com.example.timetable.data

import com.example.timetable.data.datenmodell.CalenderDay
import com.example.timetable.data.datenmodell.DaVinciResponse
import com.example.timetable.data.datenmodell.Event
import com.example.timetable.data.datenmodell.Lesson
import com.example.timetable.data.services.CalenderDayMapper
import com.example.timetable.data.services.DaVinciApi
import com.example.timetable.data.services.LessonParser
import java.io.File

class TimetableRepository(
    private val storageDir: File,
    private val api: DaVinciApi = DaVinciApi(),
    private val parser: LessonParser = LessonParser()
) {

    private var lessons: List<Lesson> = emptyList()
    private var events: List<Event> = emptyList()
    private var calenderDays: List<CalenderDay> = emptyList()

    /**
     * Initialisiert die lokalen Daten.
     * Wenn bereits eine Cache-Datei existiert, wird sie verwendet.
     * Sonst wird die JSON-Datei neu heruntergeladen.
     *
     * @return Die aktuellen Kalendertage.
     */
    fun initialize(): List<CalenderDay> {
        val cachedResponse = api.loadFromCache(storageDir)
        return if (cachedResponse != null) {
            applyResponse(cachedResponse)
        } else {
            reloadJson()
        }
    }

    /**
     * Lädt die lokale Cache-Datei explizit neu in den Speicher.
     *
     * @return Die lokalen Kalendertage oder `null`, wenn keine Cache-Datei existiert.
     */
    fun loadFromCache(): List<CalenderDay>? {
        val cachedResponse = api.loadFromCache(storageDir) ?: return null
        return applyResponse(cachedResponse)
    }

    /**
     * Lädt die JSON-Datei neu aus dem Netz, ersetzt die lokale Datei und parsed alles neu.
     *
     * @return Die aktualisierten Kalendertage.
     */
    fun reloadJson(): List<CalenderDay> {
        val response = api.downloadAndSave(storageDir)
        return applyResponse(response)
    }

    /**
     * Prüft, ob die Remote-JSON geändert wurde, und aktualisiert bei Bedarf den lokalen Stand.
     *
     * @return `true`, wenn neue Daten gespeichert wurden, sonst `false`.
     */
    fun updateJsonIfNeeded(): Boolean {
        val result = api.checkForUpdates(storageDir)
        applyResponse(result.response)
        return result.hasUpdates
    }

    /**
     * Gibt alle aktuell geladenen Lessons zurück.
     *
     * @return Alle Lessons im Speicher.
     */
    fun getAllLessons(): List<Lesson> = lessons

    /**
     * Gibt alle aktuell geladenen Events zurück.
     *
     * @return Alle Events im Speicher.
     */
    fun getAllEvents(): List<Event> = events

    /**
     * Gibt alle aktuell geladenen Kalendertage zurück.
     *
     * @return Alle Kalendertage im Speicher.
     */
    fun getAllCalenderDays(): List<CalenderDay> = calenderDays

    /**
     * Filtert Lessons nach einem Studiengangs- oder Gruppencode.
     *
     * @param groupsCode Der gesuchte Studiengangs- oder Gruppencode.
     * @return Alle passenden Lessons.
     */
    fun getLessonsByGroupsCode(groupsCode: String): List<Lesson> =
        lessons.filter { groupsCode in it.groupsCode }

    /**
     * Sucht eine Lesson ueber ihre stabile ID.
     *
     * @param lessonId Die gesuchte Lesson-ID.
     * @return Die passende Lesson oder `null`.
     */
    fun getLessonById(lessonId: String): Lesson? =
        lessons.firstOrNull { it.id == lessonId }

    /**
     * Filtert Lessons nach Titel und Studiengangs- oder Gruppencode.
     *
     * @param title Der gesuchte Lesson-Titel.
     * @param groupsCode Der gesuchte Studiengangs- oder Gruppencode.
     * @return Alle passenden Lessons.
     */
    fun getLessonsByTitleAndGroupsCode(title: String, groupsCode: String): List<Lesson> =
        lessons.filter { lesson ->
            lesson.title == title && groupsCode in lesson.groupsCode
        }

    /**
     * Gibt alle Lessons für ein bestimmtes Datum zurück.
     *
     * @param date Datum im Format `YYYY-MM-DD`.
     * @return Alle Lessons des Tages.
     */
    fun getLessonsByDate(date: String): List<Lesson> =
        lessons.filter { it.date == date }.sortedBy { it.startTime }

    /**
     * Gibt alle Events für ein bestimmtes Datum zurück.
     *
     * @param date Datum im Format `YYYY-MM-DD`.
     * @return Alle Events des Tages.
     */
    fun getEventsByDate(date: String): List<Event> =
        calenderDays.firstOrNull { it.date == date }?.events.orEmpty()

    /**
     * Gibt einen einzelnen Kalendertag zurück.
     *
     * @param date Datum im Format `YYYY-MM-DD`.
     * @return Den passenden Kalendertag oder `null`.
     */
    fun getCalenderDay(date: String): CalenderDay? =
        calenderDays.firstOrNull { it.date == date }

    /**
     * Löscht nur den geladenen Speicherzustand, nicht die Datei.
     */
    fun clearMemory() {
        lessons = emptyList()
        events = emptyList()
        calenderDays = emptyList()
    }

    private fun applyResponse(response: DaVinciResponse): List<CalenderDay> {
        lessons = parser.parseLessons(response.lessonTimes)
        events = parser.parseEvents(response.eventTimes)
        calenderDays = CalenderDayMapper.build(lessons, events)
        return calenderDays
    }
}