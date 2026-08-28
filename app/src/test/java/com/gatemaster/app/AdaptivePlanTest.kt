package com.gatemaster.app

import com.gatemaster.app.core.model.AdaptivePlan
import com.gatemaster.app.core.model.PracticeReason
import com.gatemaster.app.core.model.TopicHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scheduler.
 *
 * This is the only part of the app that tells the user what to do, so it is
 * worth pinning down: a plausible-looking recommendation that is quietly wrong
 * would send someone revising the wrong subject for a week, and nothing else in
 * the app would notice.
 */
class AdaptivePlanTest {

    private val now = 1_767_225_600_000L // 2026-01-01
    private fun daysAgo(days: Int) = now - days * 24L * 60 * 60 * 1000

    // --- Mastery ------------------------------------------------------------

    @Test
    fun `one lucky answer does not count as mastery`() {
        // The reason for the prior. Raw accuracy would call this 100%, drop the
        // topic for three weeks, and quietly hide a topic the user has barely
        // seen.
        val raw = 1.0
        val smoothed = AdaptivePlan.mastery(attempted = 1, correct = 1)

        assertTrue("expected well below $raw, was $smoothed", smoothed < 0.6)
    }

    @Test
    fun `one unlucky answer does not count as a catastrophe`() {
        val smoothed = AdaptivePlan.mastery(attempted = 1, correct = 0)
        assertTrue("expected well above 0, was $smoothed", smoothed > 0.2)
    }

    @Test
    fun `mastery converges on the real accuracy as evidence accumulates`() {
        val few = AdaptivePlan.mastery(attempted = 4, correct = 4)
        val many = AdaptivePlan.mastery(attempted = 60, correct = 60)

        assertTrue(many > few)
        assertTrue("60 for 60 should read as near-mastery, was $many", many > 0.9)
    }

    // --- Intervals ----------------------------------------------------------

    @Test
    fun `a known topic waits longer than an unknown one`() {
        val weak = AdaptivePlan.intervalDays(0.1)
        val strong = AdaptivePlan.intervalDays(0.95)

        assertTrue(weak < 2.0)
        assertTrue(strong > 15.0)
    }

    // --- Priority -----------------------------------------------------------

    @Test
    fun `a weak topic outranks a strong one seen at the same time`() {
        val ranked = AdaptivePlan.prioritise(
            listOf(
                topic("strong", attempted = 20, correct = 19, last = daysAgo(3)),
                topic("weak", attempted = 20, correct = 4, last = daysAgo(3)),
            ),
            now = now,
        )

        assertEquals("weak", ranked.first().topicId)
        assertEquals(PracticeReason.WEAK, ranked.first().reason)
    }

    @Test
    fun `a strong topic left long enough comes back up`() {
        // The spaced-repetition half. Something known well in October is still
        // worth a question in January.
        val ranked = AdaptivePlan.prioritise(
            listOf(
                topic("strong-but-stale", attempted = 20, correct = 18, last = daysAgo(90)),
                topic("strong-and-fresh", attempted = 20, correct = 18, last = daysAgo(1)),
            ),
            now = now,
        )

        assertEquals("strong-but-stale", ranked.first().topicId)
        assertEquals(PracticeReason.DUE, ranked.first().reason)
    }

    @Test
    fun `a heavier subject outranks a lighter one that is equally weak`() {
        val ranked = AdaptivePlan.prioritise(
            listOf(
                topic("light", subject = "toc", attempted = 10, correct = 4, last = daysAgo(5)),
                topic("heavy", subject = "aptitude", attempted = 10, correct = 4, last = daysAgo(5)),
            ),
            subjectWeights = mapOf("aptitude" to 15, "toc" to 3),
            now = now,
        )

        assertEquals("heavy", ranked.first().topicId)
    }

    @Test
    fun `a light subject is still scheduled, just less`() {
        // Weight is half flat on purpose: a 3-mark subject that never came up
        // would be a scheduler that decides some of the exam does not matter.
        val ranked = AdaptivePlan.prioritise(
            listOf(topic("light", subject = "toc", attempted = 10, correct = 2, last = daysAgo(30))),
            subjectWeights = mapOf("aptitude" to 15, "toc" to 3),
            now = now,
        )

        assertTrue(ranked.single().score > 0.0)
    }

    @Test
    fun `an unpractised topic is scheduled but does not outrank a real weakness`() {
        val ranked = AdaptivePlan.prioritise(
            listOf(
                topic("never-seen", attempted = 0, correct = 0, last = 0),
                topic("known-bad", attempted = 30, correct = 3, last = daysAgo(10)),
            ),
            now = now,
        )

        assertEquals("known-bad", ranked.first().topicId)
        assertEquals(PracticeReason.UNSEEN, ranked.last().reason)
        assertTrue("an unseen topic must still be schedulable", ranked.last().score > 0.0)
    }

    @Test
    fun `a topic just practised is ranked last but not dropped`() {
        val ranked = AdaptivePlan.prioritise(
            listOf(
                topic("just-done", attempted = 10, correct = 5, last = now),
                topic("last-week", attempted = 10, correct = 5, last = daysAgo(7)),
            ),
            now = now,
        )

        assertEquals("last-week", ranked.first().topicId)
        assertTrue(ranked.last().score > 0.0)
    }

    @Test
    fun `being years overdue does not crowd out everything else`() {
        // Without the cap, one ancient topic would take the whole set for ever.
        val ancient = AdaptivePlan.prioritise(
            listOf(topic("ancient", attempted = 10, correct = 5, last = daysAgo(3650))),
            now = now,
        ).single()

        val merelyOld = AdaptivePlan.prioritise(
            listOf(topic("old", attempted = 10, correct = 5, last = daysAgo(60))),
            now = now,
        ).single()

        assertEquals(ancient.score, merelyOld.score, 0.0001)
    }

    // --- Allocation ---------------------------------------------------------

    @Test
    fun `questions are spread across topics, not dumped on the worst one`() {
        val ranked = AdaptivePlan.prioritise(
            (1..8).map { topic("t$it", attempted = 10, correct = it, last = daysAgo(10)) },
            now = now,
        )

        val allocation = AdaptivePlan.allocate(ranked, questionCount = 15)

        assertEquals(15, allocation.values.sum())
        assertTrue("expected several topics, got ${allocation.size}", allocation.size >= 3)
        assertTrue("no topic may be allocated zero", allocation.values.all { it >= 1 })
    }

    @Test
    fun `the worst topic gets the biggest share`() {
        val ranked = AdaptivePlan.prioritise(
            listOf(
                topic("awful", attempted = 20, correct = 1, last = daysAgo(20)),
                topic("fine", attempted = 20, correct = 18, last = daysAgo(2)),
            ),
            now = now,
        )

        val allocation = AdaptivePlan.allocate(ranked, questionCount = 10)
        assertTrue(allocation.getValue("awful") > allocation.getValue("fine"))
    }

    @Test
    fun `the allocation always totals exactly what was asked for`() {
        // Rounding a proportional split is where an off-by-one lives, and a set
        // that quietly holds 14 questions instead of 15 is the kind of bug
        // nobody reports and everybody notices.
        for (count in 1..30) {
            val ranked = AdaptivePlan.prioritise(
                (1..10).map { topic("t$it", attempted = 10, correct = it % 10, last = daysAgo(it)) },
                now = now,
            )
            assertEquals(
                "asked for $count",
                count,
                AdaptivePlan.allocate(ranked, questionCount = count).values.sum(),
            )
        }
    }

    @Test
    fun `more topics than questions never allocates a topic zero questions`() {
        val ranked = AdaptivePlan.prioritise(
            (1..20).map { topic("t$it", attempted = 5, correct = 1, last = daysAgo(it)) },
            now = now,
        )

        val allocation = AdaptivePlan.allocate(ranked, questionCount = 3)
        assertEquals(3, allocation.values.sum())
        assertEquals(3, allocation.size)
    }

    @Test
    fun `no history means no plan rather than an empty set of questions`() {
        assertTrue(AdaptivePlan.prioritise(emptyList(), now = now).isEmpty())
        assertTrue(AdaptivePlan.allocate(emptyList(), questionCount = 10).isEmpty())
    }

    private fun topic(
        id: String,
        subject: String = "os",
        attempted: Int,
        correct: Int,
        last: Long,
    ) = TopicHistory(id, subject, attempted, correct, last)
}
