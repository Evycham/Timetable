package com.example.timetable.data

import android.content.Context
import androidx.room.withTransaction
import com.example.timetable.data.datenmodell.CalenderDay
import com.example.timetable.data.datenmodell.DaVinciResponse
import com.example.timetable.data.datenmodell.Event
import com.example.timetable.data.datenmodell.Lesson
import com.example.timetable.data.local.TimetableDatabase
import com.example.timetable.data.local.toEntity
import com.example.timetable.data.local.toModel
import com.example.timetable.data.services.CalenderDayMapper
import com.example.timetable.data.services.DaVinciApi
import com.example.timetable.data.services.LessonParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File

class TimetableRepository(
    context: Context,
    private val storageDir: File,
    private val api: DaVinciApi = DaVinciApi(),
    private val parser: LessonParser = LessonParser(),
    private val database: TimetableDatabase = TimetableDatabase.getInstance(context)
) {

    private val lessonDao = database.lessonDao()
    private val eventDao = database.eventDao()

    private var lessons: List<Lesson> = emptyList()
    private var events: List<Event> = emptyList()
    private var calenderDays: List<CalenderDay> = emptyList()

    val lessonsFlow: Flow<List<Lesson>> = lessonDao.observeAll().map { entities ->
        entities.map { entity -> entity.toModel() }
    }

    val eventsFlow: Flow<List<Event>> = eventDao.observeAll().map { entities ->
        entities.map { entity -> entity.toModel() }
    }

    val calenderDaysFlow: Flow<List<CalenderDay>> = combine(lessonsFlow, eventsFlow) { dbLessons, dbEvents ->
        CalenderDayMapper.build(dbLessons, dbEvents)
    }

    /**
     * Initialisiert die lokalen Daten.
     * Wenn bereits eine DB existiert, wird sie verwendet.
     * Sonst wird erst die lokale Raw-JSON, danach das Netz verwendet.
     *
     * @return Die aktuellen Kalendertage.
     */
    suspend fun initialize(): List<CalenderDay> {
        if (hasDatabaseData()) {
            return refreshMemoryFromDatabase()
        }

        val cachedResponse = api.loadFromCache(storageDir)
        return if (cachedResponse != null) {
            persistAndApplyResponse(cachedResponse)
        } else {
            reloadJson()
        }
    }

    /**
     * Lädt die lokale Raw-JSON-Datei neu, parsed sie und ersetzt den DB-Inhalt.
     *
     * @return Die lokalen Kalendertage oder `null`, wenn keine Cache-Datei existiert.
     */
    suspend fun loadFromCache(): List<CalenderDay>? {
        val cachedResponse = api.loadFromCache(storageDir) ?: return null
        return persistAndApplyResponse(cachedResponse)
    }

    /**
     * Lädt die JSON-Datei neu aus dem Netz, ersetzt Raw-Cache und DB und aktualisiert den Speicher.
     *
     * @return Die aktualisierten Kalendertage.
     */
    suspend fun reloadJson(): List<CalenderDay> {
        val response = api.downloadAndSave(storageDir)
        return persistAndApplyResponse(response)
    }

    /**
     * Prüft, ob die Remote-JSON geändert wurde, und aktualisiert bei Bedarf den lokalen Stand.
     * Bei einem Offline-Fehler bleibt der letzte DB-Stand verfügbar.
     *
     * @return `true`, wenn neue Daten gespeichert wurden, sonst `false`.
     */
    suspend fun updateJsonIfNeeded(): Boolean {
        return try {
            val result = api.checkForUpdates(storageDir)
            if (result.hasUpdates || !hasDatabaseData()) {
                persistAndApplyResponse(result.response)
            } else {
                refreshMemoryFromDatabase()
            }
            result.hasUpdates
        } catch (exception: Exception) {
            if (hasDatabaseData()) {
                refreshMemoryFromDatabase()
                false
            } else {
                throw exception
            }
        }
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
     * Löscht nur den geladenen Speicherzustand, nicht DB oder Datei.
     */
    fun clearMemory() {
        lessons = emptyList()
        events = emptyList()
        calenderDays = emptyList()
    }

    private suspend fun hasDatabaseData(): Boolean =
        lessonDao.count() > 0 || eventDao.count() > 0

    private suspend fun refreshMemoryFromDatabase(): List<CalenderDay> {
        val dbLessons = lessonDao.getAll().map { entity -> entity.toModel() }
        val dbEvents = eventDao.getAll().map { entity -> entity.toModel() }
        return applyModels(dbLessons, dbEvents)
    }

    private suspend fun persistAndApplyResponse(response: DaVinciResponse): List<CalenderDay> {
        val parsedLessons = parser.parseLessons(response.lessonTimes)
        val parsedEvents = parser.parseEvents(response.eventTimes)

        database.withTransaction {
            lessonDao.clear()
            eventDao.clear()
            if (parsedLessons.isNotEmpty()) {
                lessonDao.insertAll(parsedLessons.map { lesson -> lesson.toEntity() })
            }
            if (parsedEvents.isNotEmpty()) {
                eventDao.insertAll(parsedEvents.map { event -> event.toEntity() })
            }
        }

        return applyModels(parsedLessons, parsedEvents)
    }

    private fun applyModels(parsedLessons: List<Lesson>, parsedEvents: List<Event>): List<CalenderDay> {
        lessons = parsedLessons
        events = parsedEvents
        calenderDays = CalenderDayMapper.build(lessons, events)
        return calenderDays
    }
}
