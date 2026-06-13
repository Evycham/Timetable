package com.example.timetable.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.timetable.data.local.db.entities.ExtraLessonEntity
import com.example.timetable.data.local.db.entities.HiddenLessonEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO für den Zugriff auf benutzerspezifische Filterregeln.
 *
 * Verwaltet die SQLite-Tabellen `extra_lessons` (Fremdmodule) und `hidden_lessons` (ausgeblendete Vorlesungsstunden).
 * Diese Regeln wurden aus dem Preferences DataStore hierher migriert, um Fremdschlüsselbeziehungen (CASCADE onDelete)
 * auf [com.example.timetable.data.local.db.entities.LessonEntity] zu ermöglichen und komplexe Filterungen direkt in der DB-Engine durchführen zu können.
 */
@Dao
interface UserRulesDao {
    /**
     * Beobachtet alle hinzugefügten Fremdmodule als reaktiven Stream.
     *
     * @return Ein [kotlinx.coroutines.flow.Flow] mit der Liste aller aktuellen [ExtraLessonEntity]-Regeln.
     */
    @Query("SELECT * FROM extra_lessons")
    fun observeExtraLessons(): Flow<List<ExtraLessonEntity>>

    /**
     * Ruft alle hinzugefügten Fremdmodule einmalig ab.
     * Nützlich für Hintergrundprüfungen wie z. B. Benachrichtigungen (Alerts).
     *
     * @return Eine Liste aller [ExtraLessonEntity]-Regeln.
     */
    @Query("SELECT * FROM extra_lessons")
    suspend fun getExtraLessons(): List<ExtraLessonEntity>

    /**
     * Fügt eine neue Fremdmodul-Regel hinzu.
     * Ersetzt Duplikate bei Konflikten.
     *
     * @param rule Die einzufügende [ExtraLessonEntity].
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtra(rule: ExtraLessonEntity)

    /**
     * Löscht eine kursweite Fremdmodul-Auswahl (alle Termine eines Kurses für einen Studiengang).
     *
     * @param title Der Titel des Kurses.
     * @param groupsCode Die Studiengruppe, für die der Kurs ausgewählt wurde.
     */
    @Query("DELETE FROM extra_lessons WHERE title = :title AND groupsCode = :groupsCode AND lessonId IS NULL")
    suspend fun deleteExtraCourse(title: String, groupsCode: String)

    /**
     * Löscht ein spezifisches Fremdmodul-Einzelereignis anhand seiner Stunden-ID.
     *
     * @param lessonId Die stabile ID des gelöschten Termins.
     */
    @Query("DELETE FROM extra_lessons WHERE lessonId = :lessonId")
    suspend fun deleteExtraLessonById(lessonId: String)

    /**
     * Beobachtet alle ausgeblendeten Termine/Kurse als reaktiven Stream.
     *
     * @return Ein [Flow] mit der Liste aller aktuellen [HiddenLessonEntity]-Regeln.
     */
    @Query("SELECT * FROM hidden_lessons")
    fun observeHiddenLessons(): Flow<List<HiddenLessonEntity>>

    /**
     * Ruft alle ausgeblendeten Termine/Kurse einmalig ab.
     *
     * @return Eine Liste aller [HiddenLessonEntity]-Regeln.
     */
    @Query("SELECT * FROM hidden_lessons")
    suspend fun getHiddenLessons(): List<HiddenLessonEntity>

    /**
     * Fügt eine neue Ausblendungsregel hinzu.
     * Ersetzt Duplikate bei Konflikten.
     *
     * @param rule Die einzufügende [HiddenLessonEntity].
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHidden(rule: HiddenLessonEntity)

    /**
     * Löscht eine kursweite Ausblendungsregel (blendet einen Kurs für einen Studiengang wieder ein).
     *
     * @param title Der Titel des Kurses.
     * @param groupsCode Die Studiengruppe, für die der Kurs ausgeblendet war.
     */
    @Query("DELETE FROM hidden_lessons WHERE title = :title AND groupsCode = :groupsCode AND lessonId IS NULL")
    suspend fun deleteHiddenCourse(title: String, groupsCode: String)

    /**
     * Löscht eine spezifische Einzeltermin-Ausblendung anhand ihrer Stunden-ID (blendet den Termin wieder ein).
     *
     * @param lessonId Die stabile ID des wieder einzublendenden Termins.
     */
    @Query("DELETE FROM hidden_lessons WHERE lessonId = :lessonId")
    suspend fun deleteHiddenLessonById(lessonId: String)

    /**
     * Löscht alle Ausblendungsregeln eines Kurses über alle Studiengänge hinweg.
     * Nützlich, um eine Vorlesung komplett wieder im Stundenplan sichtbar zu machen.
     *
     * @param title Der Titel des Kurses.
     */
    @Query("DELETE FROM hidden_lessons WHERE title = :title")
    suspend fun deleteHiddenAllByTitle(title: String)

    /**
     * Löscht alle Fremdmodul-Regeln (z. B. beim Zurücksetzen des Profils).
     */
    @Query("DELETE FROM extra_lessons")
    suspend fun clearExtra()

    /**
     * Löscht alle Ausblendungsregeln (z. B. beim Zurücksetzen des Profils).
     */
    @Query("DELETE FROM hidden_lessons")
    suspend fun clearHidden()
}