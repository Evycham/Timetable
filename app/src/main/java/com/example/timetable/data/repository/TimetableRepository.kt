package com.example.timetable.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.timetable.data.model.CalenderDay
import com.example.timetable.data.model.Event
import com.example.timetable.data.model.Lesson
import com.example.timetable.data.local.db.entities.SyncMetadataEntity
import com.example.timetable.data.local.db.entities.LessonEntity
import com.example.timetable.data.local.db.entities.LessonRoomEntity
import com.example.timetable.data.local.db.entities.LessonTeacherEntity
import com.example.timetable.data.local.db.entities.LessonGroupEntity
import com.example.timetable.data.local.db.TimetableDatabase
import com.example.timetable.data.local.db.mapper.toEntity
import com.example.timetable.data.local.db.mapper.toModel
import com.example.timetable.data.services.CalenderDayMapper
import com.example.timetable.data.remote.DaVinciApi
import com.example.timetable.data.remote.DaVinciDownloadSnapshot
import com.example.timetable.data.remote.LessonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Der Synchronisations-Status des Repositories.
 */
sealed interface RepositorySyncState {
    data object Idle : RepositorySyncState
    data object LoadingLocal : RepositorySyncState
    data object Syncing : RepositorySyncState
    data object Ready : RepositorySyncState
    data class Error(val cause: Throwable, val hasLocalData: Boolean) : RepositorySyncState
}

/**
 * Das zentrale Daten-Repository (Single Source of Truth) der App.
 *
 * Verwaltet den Datenfluss zwischen der API ([DaVinciApi]) und der lokalen Room-Datenbank
 * ([TimetableDatabase]). Es verwaltet den Cache im Hauptspeicher für flache Abfragen und bietet
 * reaktive Flows für Vorlesungen, Feiertage und Kalendertage an.
 *
 * @param context Der Context zur Initialisierung der Datenbank.
 * @property api Der API-Client zum Herunterladen des Stundenplans.
 * @property parser Der Parser zum Verarbeiten der API-JSON-Antworten.
 * @property database Die lokale SQLite-Datenbank.
 */
class TimetableRepository(
    context: Context,
    private val api: DaVinciApi = DaVinciApi(),
    private val parser: LessonParser = LessonParser(),
    private val database: TimetableDatabase = TimetableDatabase.getInstance(context)
) {

    private val lessonDao = database.lessonDao()
    private val eventDao = database.eventDao()
    private val syncMetadataDao = database.syncMetadataDao()

    // Lokaler Arbeitsspeicher-Cache
    private var lessons: List<Lesson> = emptyList()
    private var events: List<Event> = emptyList()
    private var calenderDays: List<CalenderDay> = emptyList()

    // Interner und externer Status-Flow für Synchronisationsvorgänge
    private val _syncState = MutableStateFlow<RepositorySyncState>(RepositorySyncState.Idle)
    val syncState: StateFlow<RepositorySyncState> = _syncState
    private val initializationMutex = Mutex()

    /**
     * Ein reaktiver Stream aller im System registrierten Vorlesungsstunden inklusive Räume und Dozenten.
     */
    val lessonsFlow: Flow<List<Lesson>> =
        lessonDao.observeAllWithRelations().map { entities ->
            entities.map { entity -> entity.toModel() }
        }

    /**
     * Ein reaktiver Stream aller Feiertage und Events.
     */
    val eventsFlow: Flow<List<Event>> =
        eventDao.observeAll().map { entities ->
            entities.map { entity -> entity.toModel() }
        }

    /**
     * Ein reaktiver Stream, der Vorlesungen und Events tageweise zusammenführt.
     */
    val calenderDaysFlow: Flow<List<CalenderDay>> =
        combine(lessonsFlow, eventsFlow) { dbLessons, dbEvents ->
            CalenderDayMapper.build(dbLessons, dbEvents)
        }

    /**
     * Initialisiert die Stundenplandaten beim App-Start.
     * Nutzt lokale DB-Daten, falls vorhanden, andernfalls wird ein API-Sync durchgeführt.
     *
     * @return Die initial geladenen Kalendertage.
     */
    suspend fun initialize(): List<CalenderDay> {
        if (calenderDays.isNotEmpty() && _syncState.value == RepositorySyncState.Ready) {
            return calenderDays
        }

        return initializationMutex.withLock {
            if (calenderDays.isNotEmpty() && _syncState.value == RepositorySyncState.Ready) {
                return@withLock calenderDays
            }

            if (hasDatabaseData()) {
                _syncState.value = RepositorySyncState.LoadingLocal
                refreshMemoryFromDatabase()
            } else {
                reloadJson()
            }
        }
    }

    suspend fun prepareLaunchData(): List<CalenderDay> {
        return initializationMutex.withLock {
            if (hasDatabaseData()) {
                _syncState.value = RepositorySyncState.LoadingLocal
                refreshMemoryFromDatabase()
                updateJsonIfNeeded()
                calenderDays
            } else {
                reloadJson()
            }
        }
    }


    /**
     * Lädt die Stundenplandaten aus der lokalen Room-Datenbank in den RAM-Cache.
     *
     * @return Die geladenen Kalendertage oder `null`, falls die DB noch leer ist.
     */
    suspend fun loadFromDatabase(): List<CalenderDay>? {
        if (!hasDatabaseData()) return null
        _syncState.value = RepositorySyncState.LoadingLocal
        return refreshMemoryFromDatabase()
    }

    @Deprecated("Use loadFromDatabase() because the source is Room, not a raw cache.")
    suspend fun loadFromCache(): List<CalenderDay>? =
        loadFromDatabase()

    /**
     * Lädt die JSON-Stundenplandatei von der API herunter, speichert sie relational
     * in SQLite ab und aktualisiert den Arbeitsspeicher.
     *
     * @return Die neu synchronisierten Kalendertage.
     */
    suspend fun reloadJson(): List<CalenderDay> {
        _syncState.value = RepositorySyncState.Syncing
        return try {
            val snapshot = api.downloadSnapshot()
            persistAndApplySnapshot(snapshot)
        } catch (exception: Exception) {
            _syncState.value = RepositorySyncState.Error(
                cause = exception,
                hasLocalData = hasDatabaseData()
            )
            throw exception
        }
    }

    /**
     * Prüft, ob ein Update des Stundenplans notwendig ist (Vergleich des HTTP-Hashs).
     *
     * Falls ja, wird der Plan heruntergeladen und relational gespeichert.
     * Falls nein, wird nur der RAM-Cache aktualisiert.
     *
     * @return `true`, wenn neue Daten gespeichert wurden, andernfalls `false`.
     */
    suspend fun updateJsonIfNeeded(): Boolean {
        return try {
            val snapshot = api.downloadSnapshot()
            val currentMetadata = syncMetadataDao.get()
            val hasUpdates = currentMetadata?.jsonHash != snapshot.jsonHash || !hasDatabaseData()

            if (hasUpdates) {
                persistAndApplySnapshot(snapshot)
            } else {
                _syncState.value = RepositorySyncState.LoadingLocal
                refreshMemoryFromDatabase()
            }

            hasUpdates
        } catch (exception: Exception) {
            if (hasDatabaseData()) {
                _syncState.value = RepositorySyncState.Error(
                    cause = exception,
                    hasLocalData = true
                )
                _syncState.value = RepositorySyncState.LoadingLocal
                refreshMemoryFromDatabase()
                false
            } else {
                _syncState.value = RepositorySyncState.Error(
                    cause = exception,
                    hasLocalData = false
                )
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
        val dbLessons = lessonDao.getAllWithRelations().map { entity -> entity.toModel() }
        val dbEvents = eventDao.getAll().map { entity -> entity.toModel() }
        return applyModels(dbLessons, dbEvents).also {
            _syncState.value = RepositorySyncState.Ready
        }
    }

    /**
     * Schreibt den heruntergeladenen API-Snapshot relational in die Datenbank.
     *
     * HINWEIS:
     * Wir nutzen `database.withTransaction`, damit das Leeren der alten Daten und das
     * relationale Einfügen der neuen Daten atomar geschieht. Sollte ein Fehler auftreten,
     * wird der alte Zustand automatisch wiederhergestellt (Rollback).
     */
    private suspend fun persistAndApplySnapshot(snapshot: DaVinciDownloadSnapshot): List<CalenderDay> {
        // 1. API-Stundenplandaten im Hintergrund-Thread parsen
        val parsedLessons = withContext(Dispatchers.Default) {
            parser.parseLessons(snapshot.response.lessonTimes)
        }
        val parsedEvents = withContext(Dispatchers.Default) {
            parser.parseEvents(snapshot.response.eventTimes)
        }

        // 2. transaktional in die relationale SQLite-DB schreiben
        database.withTransaction {
            // A: Vorherige Einträge löschen (Löscht kaskadierend auch alte Relationen)
            lessonDao.clear()
            eventDao.clear()

            // B: Alle geparsten Vorlesungen relational einfügen
            parsedLessons.forEach { lesson ->
                // I: Basis-LessonEntity schreiben
                lessonDao.insert(
                    LessonEntity(
                        id = lesson.id, // Stabile ID (lessonRef#date oder Fallback-Hash)
                        title = lesson.title,
                        date = lesson.date,
                        startTime = lesson.startTime,
                        endTime = lesson.endTime,
                        building = lesson.building,
                        changeCaption = lesson.change?.caption,
                        changeReasonType = lesson.change?.reasonType,
                        changeModified = lesson.change?.modified
                    )
                )

                // II: Verknüpfte Räume in Zwischentabelle schreiben (1:N / N:M)
                if (!lesson.rooms.isNullOrEmpty()) {
                    lessonDao.insertRooms(lesson.rooms.map { LessonRoomEntity(lesson.id, it) })
                }
                // III: Verknüpfte Dozenten schreiben
                if (!lesson.teacher.isNullOrEmpty()) {
                    lessonDao.insertTeachers(lesson.teacher.map {
                        LessonTeacherEntity(
                            lesson.id,
                            it
                        )
                    })
                }
                // IV: Studiengänge (classCodes) schreiben
                lessonDao.insertGroups(lesson.groupsCode.map { LessonGroupEntity(lesson.id, it) })
            }

            // C: Feiertage eintragen
            if (parsedEvents.isNotEmpty()) {
                eventDao.insertAll(parsedEvents.map { event -> event.toEntity() })
            }

            // D: Synchronisations-Metadaten aktualisieren
            syncMetadataDao.upsert(
                SyncMetadataEntity(
                    jsonHash = snapshot.jsonHash,
                    jsonSize = snapshot.jsonSize,
                    syncedAtMillis = System.currentTimeMillis()
                )
            )
        }

        // 3. Arbeitsspeicher-Cache aktualisieren und Ready-Status setzen
        return applyModels(parsedLessons, parsedEvents).also {
            _syncState.value = RepositorySyncState.Ready
        }
    }

    private fun applyModels(
        parsedLessons: List<Lesson>,
        parsedEvents: List<Event>
    ): List<CalenderDay> {
        lessons = parsedLessons
        events = parsedEvents
        calenderDays = CalenderDayMapper.build(lessons, events)
        return calenderDays
    }
}
