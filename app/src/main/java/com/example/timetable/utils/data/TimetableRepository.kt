package com.example.timetable.utils.data

import android.content.Context
import androidx.room.withTransaction
import com.example.timetable.utils.data.datenmodell.CalenderDay
import com.example.timetable.utils.data.datenmodell.Event
import com.example.timetable.utils.data.datenmodell.Lesson
import com.example.timetable.utils.data.local.SyncMetadataEntity
import com.example.timetable.utils.data.local.TimetableDatabase
import com.example.timetable.utils.data.local.toEntity
import com.example.timetable.utils.data.local.toModel
import com.example.timetable.utils.data.services.CalenderDayMapper
import com.example.timetable.utils.data.services.DaVinciApi
import com.example.timetable.utils.data.services.DaVinciDownloadSnapshot
import com.example.timetable.utils.data.services.LessonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface RepositorySyncState {
    data object Idle : RepositorySyncState
    data object LoadingLocal : RepositorySyncState
    data object Syncing : RepositorySyncState
    data object Ready : RepositorySyncState
    data class Error(val cause: Throwable, val hasLocalData: Boolean) : RepositorySyncState
}

class TimetableRepository(
    context: Context,
    private val api: com.example.timetable.utils.data.services.DaVinciApi = _root_ide_package_.com.example.timetable.utils.data.services.DaVinciApi(),
    private val parser: com.example.timetable.utils.data.services.LessonParser = _root_ide_package_.com.example.timetable.utils.data.services.LessonParser(),
    private val database: com.example.timetable.utils.data.local.TimetableDatabase = _root_ide_package_.com.example.timetable.utils.data.local.TimetableDatabase.getInstance(context)
) {

    private val lessonDao = database.lessonDao()
    private val eventDao = database.eventDao()
    private val syncMetadataDao = database.syncMetadataDao()

    private var lessons: List<com.example.timetable.utils.data.datenmodell.Lesson> = emptyList()
    private var events: List<com.example.timetable.utils.data.datenmodell.Event> = emptyList()
    private var calenderDays: List<com.example.timetable.utils.data.datenmodell.CalenderDay> = emptyList()

    private val _syncState = MutableStateFlow<com.example.timetable.utils.data.RepositorySyncState>(
        _root_ide_package_.com.example.timetable.utils.data.RepositorySyncState.Idle)
    val syncState: StateFlow<com.example.timetable.utils.data.RepositorySyncState> = _syncState

    val lessonsFlow: Flow<List<com.example.timetable.utils.data.datenmodell.Lesson>> = lessonDao.observeAll().map { entities ->
        entities.map { entity -> entity.toModel() }
    }

    val eventsFlow: Flow<List<com.example.timetable.utils.data.datenmodell.Event>> = eventDao.observeAll().map { entities ->
        entities.map { entity -> entity.toModel() }
    }

    val calenderDaysFlow: Flow<List<com.example.timetable.utils.data.datenmodell.CalenderDay>> = combine(lessonsFlow, eventsFlow) { dbLessons, dbEvents ->
        _root_ide_package_.com.example.timetable.utils.data.services.CalenderDayMapper.build(dbLessons, dbEvents)
    }

    /**
     * Initialisiert die lokalen Daten.
     * Wenn bereits eine DB existiert, wird sie verwendet.
     * Sonst wird aus dem Netz geladen und direkt in Room gespeichert.
     *
     * @return Die aktuellen Kalendertage.
     */
    suspend fun initialize(): List<com.example.timetable.utils.data.datenmodell.CalenderDay> {
        return if (hasDatabaseData()) {
            _syncState.value = _root_ide_package_.com.example.timetable.utils.data.RepositorySyncState.LoadingLocal
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
    suspend fun loadFromDatabase(): List<com.example.timetable.utils.data.datenmodell.CalenderDay>? {
        if (!hasDatabaseData()) return null
        _syncState.value = _root_ide_package_.com.example.timetable.utils.data.RepositorySyncState.LoadingLocal
        return refreshMemoryFromDatabase()
    }

    @Deprecated("Use loadFromDatabase() because the source is Room, not a raw cache.")
    suspend fun loadFromCache(): List<com.example.timetable.utils.data.datenmodell.CalenderDay>? = loadFromDatabase()

    /**
     * Lädt die JSON-Datei neu aus dem Netz, parsed sie und ersetzt den DB-Inhalt.
     *
     * @return Die aktualisierten Kalendertage.
     */
    suspend fun reloadJson(): List<com.example.timetable.utils.data.datenmodell.CalenderDay> {
        _syncState.value = _root_ide_package_.com.example.timetable.utils.data.RepositorySyncState.Syncing
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
        val dbLessons = lessonDao.getAll().map { entity -> entity.toModel() }
        val dbEvents = eventDao.getAll().map { entity -> entity.toModel() }
        return applyModels(dbLessons, dbEvents).also {
            _syncState.value = RepositorySyncState.Ready
        }
    }

    private suspend fun persistAndApplySnapshot(snapshot: DaVinciDownloadSnapshot): List<CalenderDay> {
        val parsedLessons = withContext(Dispatchers.Default) {
            parser.parseLessons(snapshot.response.lessonTimes)
        }
        val parsedEvents = withContext(Dispatchers.Default) {
            parser.parseEvents(snapshot.response.eventTimes)
        }

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

        return applyModels(parsedLessons, parsedEvents).also {
            _syncState.value = RepositorySyncState.Ready
        }
    }

    private fun applyModels(parsedLessons: List<Lesson>, parsedEvents: List<Event>): List<CalenderDay> {
        lessons = parsedLessons
        events = parsedEvents
        calenderDays = CalenderDayMapper.build(lessons, events)
        return calenderDays
    }
}
