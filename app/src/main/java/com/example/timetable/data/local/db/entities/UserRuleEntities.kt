package com.example.timetable.data.local.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Tabelle für vom Nutzer ausgewählte Fremdmodule (extraLessons).
 *
 * Hinweis:
 * * Wenn `lessonId` befüllt ist, handelt es sich um eine Einzeltermin-Auswahl. Durch die stabile ID-Logik überlebt diese Regel Raumänderungen und bleibt sauber verknüpft.
 * * Ist `lessonId` nul, handelt es sich um eine kursweite Auswahl über `title` und `groupsCode`.
 */
@Entity(
    tableName = "extra_lessons",
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE // löscht die Regel automatisch, falls die Einheit komplett entfällt
        )
    ]
)
data class ExtraLessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: String? = null, // Fremdschlüssel (nullable für kursweite Regeln)
    val title: String? = null,
    val groupsCode: String? = null
)

/**
 * Tabelle für vom Nutzer ausgeblendete Vorlesungen (hiddenLessons).
 *
 * Hinweis:
 * * Wenn `lessonId` befüllt ist, handelt es sich um eine Einzeltermin-Auswahl. Durch die stabile ID-Logik überlebt diese Regel Raumänderungen und bleibt sauber verknüpft.
 * * Ist `lessonId` nul, handelt es sich um eine kursweite Auswahl über `title` und `groupsCode`.
 */
@Entity(
    tableName = "hidden_lessons",
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HiddenLessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: String? = null, // Fremdschlüssel (nullable für kursweite Regeln)
    val title: String? = null,
    val groupsCode: String? = null
)