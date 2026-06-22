package com.example.timetable.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.timetable.data.local.db.entities.LessonEntity
import com.example.timetable.data.local.db.entities.LessonGroupEntity
import com.example.timetable.data.local.db.entities.LessonRoomEntity
import com.example.timetable.data.local.db.entities.LessonTeacherEntity
import com.example.timetable.data.local.db.entities.LessonWithRelations
import kotlinx.coroutines.flow.Flow

/**
 * DAO für den Zugriff auf Vorlesungsdaten ([LessonEntity]).
 *
 * Verwaltet das Schreiben, Löschen und Filtern der normalisierten Vorlesungsdaten.
 * Da das Datenmodell in relationale Zwischentabellen aufgeteilt ist, werden Abfragen, die das
 * vollständige Beziehungsgeflecht laden, mit `@Transaction` annotiert, um eine konsistente
 * Abfrage über mehrere Tabellen hinweg zu garantieren.
 */
@Dao
interface LessonDao {
    /**
     * Beobachtet alle Einheiten inklusive Räume, Dozenten und Gruppen reaktiv.
     * Room führt automatisch JOINs über die Zwischentabellen aus.
     *
     * @return Ein [kotlinx.coroutines.flow.Flow] mit der Liste aller [LessonWithRelations]-Projektionen.
     */
    @Transaction
    @Query("SELECT * FROM lessons")
    fun observeAllWithRelations(): Flow<List<LessonWithRelations>>

    /**
     * Ruft alle Vorlesungen inklusive aller Beziehungen einmalig ab.
     * Nützlich für Hintergrundabgleiche (z. B. Alert-Detektion).
     *
     * @return Eine Liste aller [LessonWithRelations]-Projektionen.
     */
    @Transaction
    @Query("SELECT * FROM lessons")
    suspend fun getAllWithRelations(): List<LessonWithRelations>

    /**
     * Findet eine spezifische Einheit anhand ihrer stabilen ID.
     *
     * @param id Die ID der gesuchten Vorlesung (z. B. "lessonRef#date").
     * @return Die [LessonWithRelations] oder `null`, falls kein Eintrag gefunden wurde.
     */
    @Transaction
    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LessonWithRelations?

    /**
     * Gibt die Gesamtzahl der gespeicherten Einheitseinträge zurück.
     *
     * @return Die Zeilenanzahl der Tabelle `lessons`.
     */
    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun count(): Int

    /**
     * Fügt den Basis-Eintrag einer einzelnen Einheit ein.
     * Überschreibt vorhandene Einträge bei Konflikten.
     *
     * @param lesson Die einzufügende [LessonEntity].
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lesson: LessonEntity)

    /**
     * Fügt Raumzuordnungen für eine Vorlesung in die Zwischentabelle ein.
     *
     * @param rooms Eine Liste von [LessonRoomEntity]-Zuordnungen.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<LessonRoomEntity>)

    /**
     * Fügt Dozentenzuordnungen für eine Vorlesung in die Zwischentabelle ein.
     *
     * @param teachers Eine Liste von [LessonTeacherEntity]-Zuordnungen.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<LessonTeacherEntity>)

    /**
     * Fügt Studiengangszuordnungen für eine Vorlesung in die Zwischentabelle ein.
     *
     * @param groups Eine Liste von [LessonGroupEntity]-Zuordnungen.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<LessonGroupEntity>)

    /**
     * Löscht alle Vorlesungen aus der Tabelle `lessons`.
     * Durch die kaskadierenden Fremdschlüssel (`onDelete = ForeignKey.CASCADE`) werden
     * verknüpfte Einträge in den Zwischentabellen automatisch mitgelöscht.
     */
    @Query("DELETE FROM lessons")
    suspend fun clear()

    /**
     * Generiert den nutzerspezifischen, gefilterten Stundenplan direkt auf DB-Ebene.
     *
     * **Erklärung der Logik (JOINs und Unterabfragen):**
     * * `INNER JOIN lesson_groups lg ON l.id = lg.lessonId`: Verknüpft die Vorlesungen
     *   mit ihren Studiengängen, um filtern zu können.
     * * `WHERE (...)`: Bestimmt, welche Einträge in den Stundenplan des Nutzers kommen.
     *   * `lg.groupsCode = :userGroupsCode`: Basis-Eintrag. Die Stunde gehört zum primär
     *     gewählten Studiengang des Nutzers.
     *   * `OR EXISTS (SELECT 1 FROM extra_lessons el ...)`: Fremdmodule. Füge Stunden hinzu,
     *     wenn der Nutzer sie manuell als Zusatzfach hinzugefügt hat (entweder als Einzeltermin
     *     über `el.lessonId = l.id` oder kursweit über Titel und Gruppe).
     * * `AND NOT EXISTS (SELECT 1 FROM hidden_lessons hl ...)`: Ausblendungsregeln (Blacklist).
     *   Schließe Stunden aus, wenn sie vom Nutzer ausgeblendet wurden (entweder als Einzeltermin
     *   über `hl.lessonId = l.id` oder kursweit über den Titel des Moduls).
     * * `ORDER BY l.date, l.startTime, l.title`: Sortiert das Ergebnis chronologisch nach Datum,
     *   Uhrzeit und anschließend alphabetisch nach Titel.
     *
     * @param userGroupsCode Der primäre Studiengangs-Code des Nutzers (z. B. "eti-SKIB_4").
     * @return Ein reaktiver [Flow] mit der Liste der gefilterten [LessonWithRelations]-Objekte.
     */
    @Transaction
    @Query(
        """
        select distinct l.* from lessons l
        inner join lesson_groups lg on l.id = lg.lessonId
        where (
            -- 1. Basisplan
            lg.groupsCode = :userGroupsCode
            -- 2. extralessons
            or exists (
                select 1 from extra_lessons el
                where el.lessonId = l.id
                    or (
                        el.lessonId is null 
                        and el.title = l.title 
                        and el.groupsCode = :userGroupsCode
                    )
            )
        )
        -- 3. hiddenlessons
        and not exists (
            select 1 from hidden_lessons hl
            where hl.lessonId = l.id
                or (
                    hl.lessonId is null 
                    and hl.title = l.title 
                    and (
                        hl.groupsCode is null 
                        or hl.groupsCode = :userGroupsCode
                    )
                )
        )
        order by l.date, l.startTime, l.title
    """
    )
    fun observeUserLessons(userGroupsCode: String): Flow<List<LessonWithRelations>>
}