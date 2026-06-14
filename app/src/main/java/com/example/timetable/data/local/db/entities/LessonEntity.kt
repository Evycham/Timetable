package com.example.timetable.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Repräsentiert eine Einheit (Vorlesung / Übung / Labor) in Room.
 *
 * Hinweis:
 * Um die 1. Normalform zu gewährleisten müssen alle Attribute atomar sein. `rooms`, `teacher` und `groupsCode` habe ich daher in eigene Tabellen ausgelagert. Dadurch fällt auch der TypeConverter weg.
 *
 * @property id Die stabile, eindeutige ID des Termins (generiert aus "lessonRef#date" oder Fallback-Hash)
 * @property title Der Name der Lehrveranstaltung (zB. "Mathematik 1")
 * @property date Das Datum des Termins im Format "YYYY-MM-DD" zB. "2026-06-15")
 * @property startTime Die Startzeit des Termins im Format "HH:MM" (zB. "09:45")
 * @property endTime Die Endzeit des Termins im Format "HH:MM" (zB. "11:15")
 * @property building Das Kürzel des Gebäudes (zB. "4"), falls vorhanden
 * @property changeCaption Beschreibung einer eventuellen Änderung (zB. "Raumwechsel")
 * @property changeReasonType Der Grund für eine Änderung als standardisierter Key (zB. "roomChange")
 * @property changeModified Der Zeitstempel, wann die Änderung vorgenommen wurde.
 */
@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val building: String?,
    val changeCaption: String?,
    val changeReasonType: String?,
    val changeModified: String?
)