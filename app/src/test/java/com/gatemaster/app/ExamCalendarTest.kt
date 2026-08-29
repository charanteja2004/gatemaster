package com.gatemaster.app

import com.gatemaster.app.core.model.ExamCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Which GATE the app is counting down to.
 *
 * Worth its own test because it was wrong in a way nobody would report: the
 * paper picker said "all 30 GATE 2026 papers" while the home screen counted
 * down to GATE 2027, on the same install, for eleven months of the year. Two
 * screens each answering the question separately, and one of them by hard-coded
 * literal.
 */
class ExamCalendarTest {

    @Test
    fun `the exam is always the first Saturday of February`() {
        val date = ExamCalendar.nextExamDate(LocalDate.of(2026, 6, 1))
        assertEquals(DayOfWeek.SATURDAY, date.dayOfWeek)
        assertEquals(2, date.monthValue)
        assertTrue("expected the 1st to the 7th, was ${date.dayOfMonth}", date.dayOfMonth <= 7)
    }

    @Test
    fun `before February it is this year's exam`() {
        assertEquals(2027, ExamCalendar.nextExamYear(LocalDate.of(2027, 1, 15)))
    }

    @Test
    fun `after February it rolls over to next year`() {
        // The case that produced the mismatch: past the exam, so the countdown
        // is to the following February while a literal still said this one.
        assertEquals(2028, ExamCalendar.nextExamYear(LocalDate.of(2027, 8, 29)))
    }

    @Test
    fun `on exam day itself the countdown has already moved on`() {
        // "Days to GATE" reading zero all day would be odd, and by the time
        // someone opens the app the paper is being sat.
        val examDay = ExamCalendar.nextExamDate(LocalDate.of(2027, 1, 1))
        assertEquals(examDay.year + 1, ExamCalendar.nextExamYear(examDay))
    }

    @Test
    fun `the day before the exam still counts down to it`() {
        val examDay = ExamCalendar.nextExamDate(LocalDate.of(2027, 1, 1))
        assertEquals(examDay, ExamCalendar.nextExamDate(examDay.minusDays(1)))
    }

    @Test
    fun `every year from 2026 to 2040 resolves to a February Saturday`() {
        // February's first Saturday lands on the 1st in some years and the 7th
        // in others; a loop that assumed a fixed offset would be wrong in about
        // six of these.
        for (year in 2026..2040) {
            val date = ExamCalendar.nextExamDate(LocalDate.of(year, 1, 1))
            assertEquals("year $year", year, date.year)
            assertEquals("year $year", DayOfWeek.SATURDAY, date.dayOfWeek)
            assertTrue("year $year gave ${date.dayOfMonth}", date.dayOfMonth in 1..7)
        }
    }
}
