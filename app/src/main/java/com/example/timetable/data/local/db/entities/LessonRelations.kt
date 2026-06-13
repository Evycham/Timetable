package com.example.timetable.data.local.db.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Relation

/**
 * Junction Table zur Verknüpung von Vorlesungen mit ihren Räumen.
 *
 * Jede Einheit kann in mehreren Räumen stattfinden, und jeder Raum kann von mehreren Einheiten belegt werden. Der Fremdschlüssel referenziert [LessonEntity].
 * Bei Löschen einer LessonEntity werden verknüpfte Räume automatisch kaskadierend mitgelöscht, da wir keine eigene Raumtabelle pflegen.
 *
 * @property lessonId Der Fremdschlüssel, der auf die zugehörige [LessonEntity.id] verweist.
 * @property roomCode Das eindeutige Raumkürzel (z. B. "4/302").
 */
@Entity(
    tableName = "lesson_rooms",
    primaryKeys = ["lessonId", "roomCode"],
    foreignKeys = [
        ForeignKey(
            // entity erwartet die klassen-referenz, nicht nur den typnamen
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LessonRoomEntity(
    val lessonId: String,
    val roomCode: String
)

/**
 * Junction Table zur Verknüpfung von Vorlesungen mit ihren Dozenten (Lehrpersonen).
 *
 * Jede Einheit kann von mehreren Dozenten gehalten werden, und jeder Dozent kann mehrere Einheiten halten.
 * Der Fremdschlüssel referenziert [LessonEntity].
 * Löschungen der Vorlesungsstunde werden kaskadierend an diese Tabelle weitergegeben.
 *
 * @property lessonId Der Fremdschlüssel, der auf die zugehörige [LessonEntity.id] verweist.
 * @property teacherCode Das eindeutige Kürzel des Dozenten (zB. "eti-Otto")
 */
@Entity(
    tableName = "lesson_teachers",
    primaryKeys = ["lessonId", "teacherCode"],
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LessonTeacherEntity(
    val lessonId: String,
    val teacherCode: String
)

/**
 * Junction Table zur Verknüpfung von Vorlesungen mit den teilnehmenden Studiengängen.
 *
 * Jede Einheit kann für mehrere Studiengänge angeboten werden (zB. eine gemeinsame Vorlesung für zwei Studiengänge), und jeder Studiengang hat eine Vielzahl von Vorlesungen.
 * Der Fremdschlüssel referenziert [LessonEntity.id].
 * Löschungen der Vorlesungsstunden werden kaskadierend an diese Tabelle weitergegeben.
 *
 * @property lessonId Der Fremdschlüssel, der auf die zugehörige [LessonEntity.id] verweist.
 * @property groupsCode Das Kürzel des Studiengangs (zB. "eti-SMSB_2").
 */
@Entity(
    tableName = "lesson_groups",
    primaryKeys = ["lessonId", "groupsCode"],
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LessonGroupEntity(
    val lessonId: String,
    val groupsCode: String
)

/**
 * Repräsentiert eine Vorlesung inklusive all ihrer aufgelösten relationalen Beziehungen.
 *
 * Diese Klasse dient Room als Datenübertragungsobjekt (DTO/Projection).
 * Durch die `@Relation`-Annotationen weiß Room, dass es bei der Abfrage dieser Klasse
 * automatisch zusätzliche SELECT-Statements für die Räume, Dozenten und Gruppen ausführen
 * und diese in den Listen zusammenführen soll.
 *
 * @property lesson Der eingebettete Basis-Datensatz der Vorlesung.
 * @property rooms Die Liste aller der Vorlesung zugeordneten Raumkürzel.
 * @property teachers Die Liste aller der Vorlesung zugeordneten Dozentenkürzel.
 * @property groups Die Liste aller der Vorlesung zugeordneten Studiengang-Kürzel.
 */
data class LessonWithRelations(
    @Embedded val lesson: LessonEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "lessonId",
        entity = LessonRoomEntity::class,
        projection = ["roomCode"]
    )
    val rooms: List<String>,

    @Relation(
        parentColumn = "id",
        entityColumn = "lessonId",
        entity = LessonTeacherEntity::class,
        projection = ["teacherCode"]
    )
    val teachers: List<String>,

    @Relation(
        parentColumn = "id",
        entityColumn = "lessonId",
        entity = LessonGroupEntity::class,
        projection = ["groupsCode"]
    )
    val groups: List<String>
)
