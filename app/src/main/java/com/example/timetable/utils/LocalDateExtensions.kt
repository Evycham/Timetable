package com.example.timetable.utils

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Fügt Wochentage (Montag bis Freitag) hinzu oder zieht diese ab und überspringt Wochenenden (Samstag und Sonntag).
 * Falls das Startdatum auf ein Wochenende fällt, wird es zuerst auf den darauffolgenden Montag verschoben.
 */
fun LocalDate.plusWeekdays(daysToAdd: Long): LocalDate {
    var date = this
    if (date.dayOfWeek == DayOfWeek.SATURDAY) {
        date = date.plusDays(2)
    } else if (date.dayOfWeek == DayOfWeek.SUNDAY) {
        date = date.plusDays(1)
    }

    var remaining = daysToAdd
    if (remaining > 0) {
        while (remaining > 0) {
            date = date.plusDays(1)
            if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
                remaining--
            }
        }
    } else if (remaining < 0) {
        while (remaining < 0) {
            date = date.minusDays(1)
            if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
                remaining++
            }
        }
    }
    return date
}
