package com.gatemaster.app.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

/**
 * When the next GATE is.
 *
 * This lived as a private helper inside HomeViewModel, and the paper picker
 * hard-coded "GATE 2026" instead of asking anyone -- so once February passed,
 * the picker offered "all 30 GATE 2026 papers" on the same install whose home
 * screen counted down to GATE 2027. Two screens, two answers, one of them
 * always wrong for eleven months of the year.
 *
 * A pure function of the date, so both screens get the same answer and the
 * rollover can be tested without waiting for February.
 */
object ExamCalendar {

    /**
     * GATE runs on the first two weekends of February. The first Saturday is
     * close enough for a countdown, and rolls over to next year once this
     * year's exam has passed.
     */
    fun nextExamDate(today: LocalDate = LocalDate.now()): LocalDate {
        val thisYear = firstSaturdayOfFebruary(today.year)
        return if (thisYear.isAfter(today)) thisYear else firstSaturdayOfFebruary(today.year + 1)
    }

    fun nextExamYear(today: LocalDate = LocalDate.now()): Int = nextExamDate(today).year

    private fun firstSaturdayOfFebruary(year: Int): LocalDate {
        var date = LocalDate.of(year, Month.FEBRUARY, 1)
        while (date.dayOfWeek != DayOfWeek.SATURDAY) {
            date = date.plusDays(1)
        }
        return date
    }
}
