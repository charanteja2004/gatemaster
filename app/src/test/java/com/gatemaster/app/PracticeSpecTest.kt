package com.gatemaster.app

import com.gatemaster.app.core.model.PracticeMode
import com.gatemaster.app.core.model.PracticeSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The test id is the only thing that crosses into the player, and a saved
 * attempt is filed under it. So it has to survive a round trip exactly, and it
 * has to keep reading ids written by earlier versions of the app — otherwise
 * upgrading orphans whatever test was half-finished at the time.
 */
class PracticeSpecTest {

    private fun roundTrip(spec: PracticeSpec) = PracticeSpec.parse(spec.id)

    @Test
    fun `a topic spec survives a round trip`() {
        val spec = PracticeSpec.topic("algo", "algo_quicksort")

        assertEquals("practice:topic:algo:algo_quicksort", spec.id)
        assertEquals(spec, roundTrip(spec))
    }

    @Test
    fun `a subject spec survives a round trip`() {
        val spec = PracticeSpec.subject("dbms")

        assertEquals("practice:subject:dbms", spec.id)
        assertEquals(spec, roundTrip(spec))
    }

    @Test
    fun `a chosen mix survives a round trip`() {
        val spec = PracticeSpec.mixed(listOf("algo", "os", "dbms"))

        assertEquals("practice:mixed:algo+os+dbms", spec.id)
        assertEquals(spec, roundTrip(spec))
    }

    @Test
    fun `a mix of everything is a single id, not a list that goes stale`() {
        // Naming the subjects would freeze the paper as it was the day it was
        // built; "all" keeps meaning all of them as banks are added.
        val spec = PracticeSpec.mixed()

        assertEquals("practice:mixed:all", spec.id)
        assertEquals(spec, roundTrip(spec))
        assertEquals(emptyList<String>(), roundTrip(spec)?.subjectIds)
    }

    @Test
    fun `a repeated subject is only mixed in once`() {
        assertEquals(listOf("algo"), PracticeSpec.mixed(listOf("algo", "algo")).subjectIds)
    }

    @Test
    fun `ids written before practice grew past one subject still parse`() {
        assertEquals(
            PracticeSpec.topic("algo", "algo_quicksort"),
            PracticeSpec.parse("quick:algo:algo_quicksort"),
        )
        assertEquals(PracticeSpec.subject("algo"), PracticeSpec.parse("quick:algo"))
    }

    @Test
    fun `a bundled test id is not a practice spec`() {
        assertNull(PracticeSpec.parse("aptitude-practice-1"))
        assertNull(PracticeSpec.parse(""))
    }

    @Test
    fun `a malformed practice id is refused rather than half read`() {
        assertNull(PracticeSpec.parse("practice:"))
        assertNull(PracticeSpec.parse("practice:topic:algo"))
        assertNull(PracticeSpec.parse("practice:nonsense:algo"))
        assertNull(PracticeSpec.parse("quick:"))
    }

    @Test
    fun `each mode carries the size of sitting it is meant to be`() {
        assertEquals(10, PracticeMode.TOPIC.questionLimit)
        assertEquals(20, PracticeMode.SUBJECT.questionLimit)
        assertEquals(30, PracticeMode.MIXED.questionLimit)
    }

    @Test
    fun `duration is two minutes a question, floored and capped`() {
        assertEquals(20, PracticeSpec.durationFor(10))
        assertEquals(5, PracticeSpec.durationFor(1))
        assertEquals(60, PracticeSpec.durationFor(90))
    }
}
