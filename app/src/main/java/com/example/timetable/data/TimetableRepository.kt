package com.example.timetable.data

import android.content.Context
import androidx.room.withTransaction
import com.example.timetable.data.datenmodell.CalenderDay
import com.example.timetable.data.datenmodell.Event
import com.example.timetable.data.datenmodell.Lesson
import com.example.timetable.data.local.SyncMetadataEntity
import com.example.timetable.data.local.TimetableDatabase
import com.example.timetable.data.local.toEntity
import com.example.timetable.data.local.toModel
import com.example.timetable.data.services.CalenderDayMapper
import com.example.timetable.data.services.DaVinciApi
import com.example.timetable.data.services.DaVinciDownloadSnapshot
import com.example.timetable.data.services.LessonParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TimetableRepository(
    context: Context,
    private val api: DaVinciApi = DaVinciApi(),
    private val parser: LessonParser = LessonParser(),
    private val database: TimetableDatabase = TimetableDatabase.getInstance(context)
) {

    private val lessonDao = database.lessonDao()
    private val eventDao = database.eventDao()
    private val syncMetadataDao = database.syncMetadataDao()

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
     * Sonst wird aus dem Netz geladen und direkt in Room gespeichert.
     *
     * @return Die aktuellen Kalendertage.
     */
    suspend fun initialize(): List<CalenderDay> {
        return if (hasDatabaseData()) {
            refreshMemoryFromDatabase()
        } else {
            reloadJson()
        }
    }

    /**
     * Lädt den lokalen DB-Cache explizit neu in den Speicher.
     *
     * @return Die lokalen Kalendertage oder `null`, wenn die DB noch leer ist.
     */
    suspend fun loadFromCache(): List<CalenderDay>? {
        if (!hasDatabaseData()) return null
        return refreshMemoryFromDatabase()
    }

    /**
     * Lädt die JSON-Datei neu aus dem Netz, parsed sie und ersetzt den DB-Inhalt.
     *
     * @return Die aktualisierten Kalendertage.
     */
    suspend fun reloadJson(): List<CalenderDay> {
        val snapshot = api.downloadSnapshot()
        return persistAndApplySnapshot(snapshot)
    }

    /**
     * Prüft, ob sich die Remote-JSON über den gespeicherten Hash geändert hat.
     * Bei einem Offline-Fehler bleibt der letzte DB-Stand verfügbar.
     *
     * @return `true`, wenn neue Daten gespeichert wurden, sonst `false`.
     */
    suspend fun updateJsonIfNeeded(): Boolean {
        return try {
            val snapshot = api.downloadSnapshot()
            val currentMetadata = syncMetadataDao.get()
            val hasUpdates = currentMetadata?.jsonHash != snapshot.jsonHash || !hasDatabaseData()

            if (hasUpdates) {
                persistAndApplySnapshot(snapshot)
            } else {
                refreshMemoryFromDatabase()
            }

            hasUpdates
        } catch (exception: Exception) {
            if (hasDatabaseData()) {
                refreshMemoryFromDatabase()
                false
            } else {
                throw exception
            }
        }
    }

    fun getAllLessons(): List<Lesson> = lessons

    fun getAllEvents(): List<Event> = events

    fun getAllCalenderDays(): List<CalenderDay> = calenderDays

    fun getLessonsByGroupsCode(groupsCode: String): List<Lesson> =
        lessons.filter { groupsCode in it.groupsCode }

    fun getLessonById(lessonId: String): Lesson? =
        lessons.firstOrNull { it.id == lessonId }

    fun getLessonsByTitleAndGroupsCode(title: String, groupsCode: String): List<Lesson> =
        lessons.filter { lesson ->
            lesson.title == title && groupsCode in lesson.groupsCode
        }

    fun getLessonsByDate(date: String): List<Lesson> =
        lessons.filter { it.date == date }.sortedBy { it.startTime }

    fun getEventsByDate(date: String): List<Event> =
        calenderDays.firstOrNull { it.date == date }?.events.orEmpty()

    fun getCalenderDay(date: String): CalenderDay? =
        calenderDays.firstOrNull { it.date == date }

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

    private suspend fun persistAndApplySnapshot(snapshot: DaVinciDownloadSnapshot): List<CalenderDay> {
        val parsedLessons = parser.parseLessons(snapshot.response.lessonTimes)
        val parsedEvents = parser.parseEvents(snapshot.response.eventTimes)

        database.withTransaction {
            lessonDao.clear()
            eventDao.clear()
            if (parsedLessons.isNotEmpty()) {
                lessonDao.insertAll(parsedLessons.map { lesson -> lesson.toEntity() })
            }
            if (parsedEvents.isNotEmpty()) {
                eventDao.insertAll(parsedEvents.map { event -> event.toEntity() })
            }
            syncMetadataDao.upsert(
                SyncMetadataEntity(
                    jsonHash = snapshot.jsonHash,
                    jsonSize = snapshot.jsonSize,
                    syncedAtMillis = System.currentTimeMillis()
                )
            )
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
