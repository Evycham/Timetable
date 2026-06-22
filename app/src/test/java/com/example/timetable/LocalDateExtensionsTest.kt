package com.example.timetable

import com.example.timetable.utils.plusWeekdays
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LocalDateExtensionsTest {

    @Test
    fun plusWeekdays_fromMonday_addsCorrectly() {
        val monday = LocalDate.of(2026, 6, 22)
        
        // 0 days should return same Monday
        assertEquals(LocalDate.of(2026, 6, 22), monday.plusWeekdays(0))
        
        // 1 day should return Tuesday
        assertEquals(LocalDate.of(2026, 6, 23), monday.plusWeekdays(1))
        
        // 4 days should return Friday
        assertEquals(LocalDate.of(2026, 6, 26), monday.plusWeekdays(4))
        
        // 5 days should skip weekend and return next Monday
        assertEquals(LocalDate.of(2026, 6, 29), monday.plusWeekdays(5))
        
        // 10 days should skip two weekends and return Monday in two weeks
        assertEquals(LocalDate.of(2026, 7, 6), monday.plusWeekdays(10))
    }

    @Test
    fun plusWeekdays_fromMonday_subtractsCorrectly() {
        val monday = LocalDate.of(2026, 6, 22)
        
        // -1 day should skip weekend and return Friday of previous week
        assertEquals(LocalDate.of(2026, 6, 19), monday.plusWeekdays(-1))
        
        // -5 days should return previous Monday
        assertEquals(LocalDate.of(2026, 6, 15), monday.plusWeekdays(-5))
    }

    @Test
    fun plusWeekdays_fromWeekend_shiftsToMondayFirst() {
        val saturday = LocalDate.of(2026, 6, 20)
        val sunday = LocalDate.of(2026, 6, 21)
        
        // Saturday + 0 should shift to Monday
        assertEquals(LocalDate.of(2026, 6, 22), saturday.plusWeekdays(0))
        
        // Sunday + 0 should shift to Monday
        assertEquals(LocalDate.of(2026, 6, 22), sunday.plusWeekdays(0))
        
        // Saturday + 1 should shift to Monday, then add 1 -> Tuesday
        assertEquals(LocalDate.of(2026, 6, 23), saturday.plusWeekdays(1))
        
        // Saturday - 1 should shift to Monday, then subtract 1 -> Friday
        assertEquals(LocalDate.of(2026, 6, 19), saturday.plusWeekdays(-1))
    }
}
