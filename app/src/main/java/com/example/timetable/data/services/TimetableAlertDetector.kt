package com.example.timetable.data.services

import com.example.timetable.data.local.db.entities.ExtraLessonEntity
import com.example.timetable.data.local.db.entities.HiddenLessonEntity
import com.example.timetable.data.model.Lesson

data class TimetableAlert(
    val type: AlertType,
    val lessonTitle: String,
    val date: String,
    val startTime: String,
    val detail: String
)

enum class AlertType {
    CANCELLATION,
    ROOM_CHANGE
}

class TimetableAlertDetector {
    /**
     * Ermittelt alle relevanten Änderungen (Ausfälle und Raumänderungen) für den Nutzer.
     */
    fun detectAlerts(
        oldUserLessons: List<Lesson>,
        newLessons: List<Lesson>,
        userGroupsCode: String,
        extraLessons: List<ExtraLessonEntity>,
        hiddenLessons: List<HiddenLessonEntity>
    ): List<TimetableAlert> {
        val alerts = mutableListOf<TimetableAlert>()

        // 1. filtern, welche der neuen vorlesungen laut den user regeln im plan landen würden
        val newUserLessons = newLessons.filter { lesson ->
            val isBase = lesson.groupsCode.contains(userGroupsCode)
            val isExtra = extraLessons.any { el ->
                // Entweder Match über lessonId (Einzeltermin) oder über Titel + Gruppe (kursweit)
                el.lessonId == lesson.id ||
                        (el.lessonId == null && el.title == lesson.title && lesson.groupsCode.contains(
                            el.groupsCode
                        ))
            }

            // Vorlesung einbeziehen, wenn Base oder Extra, und KEIN Hiding-Match vorliegt
            (isBase || isExtra) && !hiddenLessons.any { hl ->
                hl.lessonId == lesson.id ||
                        (hl.lessonId == null && hl.title == lesson.title && (hl.groupsCode == null || lesson.groupsCode.contains(
                            hl.groupsCode
                        )))
            }
        }

        // 2. abgesagte vorlesungen: war im alten plan, fehlt neuen komplett (nach koordinaten)
        for (oldLesson in oldUserLessons) {
            val existsInNew = newLessons.any { newLesson ->
                newLesson.title == oldLesson.title &&
                        newLesson.date == oldLesson.date &&
                        newLesson.startTime == oldLesson.startTime &&
                        newLesson.groupsCode.intersect(oldLesson.groupsCode).isNotEmpty()
            }
            if (!existsInNew) {
                alerts.add(
                    TimetableAlert(
                        type = AlertType.CANCELLATION,
                        lessonTitle = oldLesson.title,
                        date = oldLesson.date,
                        startTime = oldLesson.startTime,
                        detail = "Die Einheit wurde abgesagt."
                    )
                )
            }
        }

        // 3. raumänderungen: vorlesung existiert in beiden, aber der raum hat sich geändert
        for (oldLesson in oldUserLessons) {
            val matchingNew = newUserLessons.firstOrNull { newLesson ->
                newLesson.title == oldLesson.title &&
                        newLesson.date == oldLesson.date &&
                        newLesson.startTime == oldLesson.startTime &&
                        newLesson.groupsCode.intersect(oldLesson.groupsCode).isNotEmpty()
            }
            // wenn Räume nicht mehr übereinstimmen -> raumänderung
            if (matchingNew != null && matchingNew.rooms != oldLesson.rooms) {
                val oldRoomStr = oldLesson.rooms?.joinToString(", ") ?: "Kein Raum"
                val newRoomStr = matchingNew.rooms?.joinToString(", ") ?: "Kein Raum"
                alerts.add(
                    TimetableAlert(
                        type = AlertType.ROOM_CHANGE,
                        lessonTitle = oldLesson.title,
                        date = oldLesson.date,
                        startTime = oldLesson.startTime,
                        detail = "Raumänderung von $oldRoomStr zu $newRoomStr."
                    )
                )
            }
        }

        return alerts
    }
}