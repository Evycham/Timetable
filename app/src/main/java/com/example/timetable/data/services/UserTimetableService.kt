package com.example.timetable.data.services

import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import com.example.timetable.data.model.CalenderDay
import com.example.timetable.data.model.Lesson
import com.example.timetable.data.local.preferences.UserSchedulePreferences
import com.example.timetable.data.local.db.entities.ExtraLessonEntity
import com.example.timetable.data.local.db.entities.HiddenLessonEntity
import com.example.timetable.data.local.db.TimetableDatabase
import com.example.timetable.data.local.db.mapper.toModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Service zur Verwaltung und reaktiven Filterung des persönlichen Stundenplans eines Benutzers.
 *
 * Diese Klasse bildet die Brücke zwischen dem globalen Datenbestand des [TimetableRepository],
 * den einfachen Benutzereinstellungen aus dem [UserSchedulePreferencesStore] und den
 * relational in Room persistierten Filterregeln ([com.example.timetable.data.local.db.dao.UserRulesDao]).
 *
 * **Architektur-Hinweis (SQLite-Delegation):**
 * Statt die Filterregeln im RAM mit komplexen Schleifen aufzubereiten (O(N*M)-Laufzeit),
 * delegiert dieser Service das Filtern und Sortieren über die Datenbank-Flows direkt an die
 * SQLite-Engine. Das spart Akkulaufzeit, Arbeitsspeicher und verkürzt die Ladezeiten.
 *
 * @property repository Das globale Daten-Repository für Vorlesungen und Events.
 * @property preferencesStore Speicherort für flache Einstellungen (z. B. Studiengang).
 * @property database Die lokale Room-Datenbank zur Verwaltung relationaler Tabellen und Regeln.
 */
class UserTimetableService(
    private val repository: TimetableRepository,
    private val preferencesStore: UserSchedulePreferencesStore,
    private val database: TimetableDatabase
) {

    private val lessonDao = database.lessonDao()
    private val userRulesDao = database.userRulesDao()


    /**
     * Ein reaktiver Stream der einfachen Benutzereinstellungen aus dem DataStore.
     */
    val preferencesFlow: Flow<UserSchedulePreferences> = preferencesStore.preferencesFlow

    /**
     * Beobachtet reaktiv den gewählten Studiengang des Benutzers und liefert dessen gefilterten Stundenplan.
     *
     * Nutzt unter der Haube [flatMapLatest]: Sobald sich der Studiengang (`groupsCode`) in den
     * Preferences ändert, wird die Room-Abfrage automatisch mit dem neuen Parameter neu getriggert.
     * Alle Filterregeln (Fremdmodule und Ausblendungen) werden direkt auf DB-Ebene angewendet.
     *
     * @return Ein [Flow] mit der Liste der gefilterten [Lesson]-Objekte des Nutzers.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun userLessonsFlow(): Flow<List<Lesson>> =
        preferencesFlow.flatMapLatest { prefs ->
            val code = prefs.groupsCode
            if (code != null) {
                // SQLite-basierte Filterung abfragen und in Domänenmodelle transformieren
                lessonDao.observeUserLessons(code).map { list ->
                    list.map { it.toModel() }
                }
            } else {
                flowOf(emptyList())
            }
        }


    /**
     * Beobachtet den gefilterten Stundenplan und kombiniert ihn reaktiv mit globalen Feiertagen/Events.
     *
     * @return Ein [Flow] mit der Liste von [CalenderDay]-Objekten, die direkt in der UI gerendert werden können.
     */
    fun userCalenderDaysFlow(): Flow<List<CalenderDay>> =
        combine(userLessonsFlow(), repository.eventsFlow) { lessons, events ->
            CalenderDayMapper.build(lessons, events)
        }

    /**
     * Berechnet den gefilterten Stundenplan des Benutzers synchron aus dem RAM-Cache.
     * Ermöglicht eine sofortige und korrekte Initialisierung des ViewModels ohne Ladelücke oder falschen Plan.
     */
    fun getCachedUserCalenderDays(): List<CalenderDay> {
        val code = preferencesStore.cachedPreferences.groupsCode
        return if (code != null) {
            val filteredLessons = repository.getLessonsByGroupsCode(code)
            CalenderDayMapper.build(filteredLessons, repository.getAllEvents())
        } else {
            emptyList()
        }
    }


    /**
     * Ruft die aktuellen Benutzereinstellungen einmalig ab.
     */
    suspend fun getPreferences(): UserSchedulePreferences = preferencesStore.load()

    /**
     * Setzt den Status, ob der Setup-Prozess vom Benutzer abgeschlossen wurde.
     */
    suspend fun setSetupComplete(isSetupComplete: Boolean) {
        preferencesStore.update { current -> current.copy(isSetupComplete = isSetupComplete) }
    }

    /**
     * Ändert den primären Studiengang des Benutzers.
     */
    suspend fun setGroupsCode(groupsCode: String) {
        preferencesStore.update { current -> current.copy(groupsCode = groupsCode) }
    }

    /**
     * Schließt den Setup-Prozess ab und speichert den gewählten Studiengang.
     */
    suspend fun completeSetup(groupsCode: String) {
        preferencesStore.update { current ->
            current.copy(
                isSetupComplete = true,
                groupsCode = groupsCode
            )
        }
    }

    /**
     * Fügt eine kursweite Fremdmodul-Auswahl in die SQLite-Datenbank ein.
     *
     * @param groupsCode Der Studiengang, zu dem der fremde Kurs gehört.
     * @param title Der Name der Lehrveranstaltung.
     */
    suspend fun addExtraLesson(groupsCode: String, title: String) {
        userRulesDao.insertExtra(
            ExtraLessonEntity(title = title, groupsCode = groupsCode)
        )
    }

    /**
     * Fügt einen spezifischen Einzeltermin eines Fremdmoduls über dessen stabile ID hinzu.
     *
     * @param lessonId Die ID des ausgewählten Vorlesungstermins (z. B. "lessonRef#date").
     */
    suspend fun addExtraLessonById(lessonId: String) {
        userRulesDao.insertExtra(
            ExtraLessonEntity(lessonId = lessonId)
        )
    }

    /**
     * Löscht eine kursweite Fremdmodul-Auswahl aus der Datenbank.
     */
    suspend fun removeExtraLesson(groupsCode: String, title: String) {
        userRulesDao.deleteExtraCourse(title, groupsCode)
    }

    /**
     * Löscht eine Einzeltermin-Fremdmodulauswahl aus der Datenbank.
     */
    suspend fun removeExtraLessonById(lessonId: String) {
        userRulesDao.deleteExtraLessonById(lessonId)
    }

    /**
     * Blendet einen Kurs für einen bestimmten Studiengang kursweit im Stundenplan aus.
     *
     * @param groupsCode Der Studiengang, für den die Ausblendung gilt.
     * @param title Der Name der auszublendenden Lehrveranstaltung.
     */
    suspend fun hideLesson(groupsCode: String, title: String) {
        userRulesDao.insertHidden(
            HiddenLessonEntity(title = title, groupsCode = groupsCode)
        )
    }

    /**
     * Blendet einen spezifischen Einzeltermin einer Vorlesung aus.
     *
     * @param lessonId Die stabile ID des auszublendenden Termins.
     */
    suspend fun hideLessonById(lessonId: String) {
        userRulesDao.insertHidden(
            HiddenLessonEntity(lessonId = lessonId)
        )
    }

    /**
     * Hebt die kursweite Ausblendung für eine Lehrveranstaltung wieder auf.
     */
    suspend fun showLesson(groupsCode: String, title: String) {
        userRulesDao.deleteHiddenCourse(title, groupsCode)
    }

    /**
     * Blendet einen spezifischen Einzeltermin (der zuvor ausgeblendet war) wieder ein.
     */
    suspend fun showLessonById(lessonId: String) {
        userRulesDao.deleteHiddenLessonById(lessonId)
    }

    /**
     * Blendet alle Termine eines Kurses wieder ein (löscht alle passenden Ausblendungsregeln).
     */
    suspend fun showAllLessonsByTitle(title: String) {
        userRulesDao.deleteHiddenAllByTitle(title)
    }

    /**
     * Aktiviert oder deaktiviert das dynamische Farbschema (Material You) in den Einstellungen.
     *
     * @param enabled true, wenn das dynamische Farbschema aktiviert werden soll, andernfalls false.
     */
    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        preferencesStore.update { current -> current.copy(isDynamicColorEnabled = enabled) }
    }

    /**
     * Aktiviert oder deaktiviert Benachrichtigungen bei Vorlesungsausfällen.
     *
     * @param enabled true, wenn Ausfall-Benachrichtigungen aktiv sein sollen, andernfalls false.
     */
    suspend fun setCancellationAlertEnabled(enabled: Boolean) {
        preferencesStore.update { current -> current.copy(isCancellationAlertEnabled = enabled) }
    }

    /**
     * Aktiviert oder deaktiviert Benachrichtigungen bei Raumänderungen.
     *
     * @param enabled true, wenn Raumänderungs-Benachrichtigungen aktiv sein sollen, andernfalls false.
     */
    suspend fun setRoomChangeAlertEnabled(enabled: Boolean) {
        preferencesStore.update { current -> current.copy(isRoomChangeAlertEnabled = enabled) }
    }

    /**
     * Legt die bevorzugte Schriftgröße der Anwendung fest.
     *
     * @param fontSize Die gewählte Schriftgröße ("Klein", "Mittel" oder "Groß").
     */
    suspend fun setAppFontSize(fontSize: String) {
        preferencesStore.update { current -> current.copy(appFontSize = fontSize) }
    }

    /**
     * Speichert oder aktualisiert ein zugeordnetes Emoji für ein bestimmtes Modul.
     *
     * @param title Der Name der Lehrveranstaltung bzw. des Moduls.
     * @param emoji Das neue Emoji-Symbol, das für diesen Kurs angezeigt werden soll.
     */
    suspend fun updateModuleEmoji(title: String, emoji: String) {
        preferencesStore.update { current ->
            val updated = current.moduleEmojis.toMutableMap().apply {
                put(title, emoji)
            }
            current.copy(moduleEmojis = updated)
        }
    }

    /**
     * Entfernt die Emoji-Zuordnung für ein bestimmtes Modul, so dass wieder das Standard-Emoji
     * verwendet wird.
     *
     * @param title Der Name der Lehrveranstaltung bzw. des Moduls, dessen Emoji gelöscht werden soll.
     */
    suspend fun removeModuleEmoji(title: String) {
        preferencesStore.update { current ->
            val updated = current.moduleEmojis.toMutableMap().apply {
                remove(title)
            }
            current.copy(moduleEmojis = updated)
        }
    }

    /**
     * Löscht alle Einstellungen und Filterregeln (Fremdmodule/Ausblendungen) des Benutzers.
     * Nützlich bei einem vollständigen App-Reset oder Profilwechsel.
     */
    suspend fun clearPreferences() {
        preferencesStore.clear()
        userRulesDao.clearExtra()
        userRulesDao.clearHidden()
    }
}
