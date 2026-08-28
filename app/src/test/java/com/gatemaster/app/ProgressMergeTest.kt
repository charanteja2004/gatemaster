package com.gatemaster.app

import com.gatemaster.app.core.data.TopicProgress
import com.gatemaster.app.core.data.sync.mergeProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The merge rule, which is the one real decision in sync.
 *
 * The server stores the reading document whole and never looks inside it, so
 * what happens when two phones have both been read on is decided here. It is a
 * pure function precisely so it can be pinned down like this, without a server,
 * a device, or a network.
 */
class ProgressMergeTest {

    @Test
    fun `furthest read takes the maximum, not the latest`() {
        // The case the whole rule exists for. The tablet got further; the phone
        // was opened more recently. Taking the later write would un-read half a
        // chapter because of which device happened to be picked up last.
        val tablet = topic("os-scheduling", furthest = 0.6f, lastOpened = 100)
        val phone = topic("os-scheduling", furthest = 0.3f, lastOpened = 200)

        val merged = mergeProgress(local = mapOf(phone.topicId to phone), remote = mapOf(tablet.topicId to tablet))

        assertEquals(0.6f, merged.getValue("os-scheduling").furthest, 0.001f)
        assertEquals(200, merged.getValue("os-scheduling").lastOpenedEpochMs)
    }

    @Test
    fun `a topic read to the end on either device stays read`() {
        val finished = topic("dbms-joins", furthest = 1.0f, lastOpened = 50)
        val barelyStarted = topic("dbms-joins", furthest = 0.05f, lastOpened = 900)

        val merged = mergeProgress(
            local = mapOf(barelyStarted.topicId to barelyStarted),
            remote = mapOf(finished.topicId to finished),
        )

        assertTrue(merged.getValue("dbms-joins").isRead)
    }

    @Test
    fun `a bookmark added on the more recent device is kept`() {
        val old = topic("algo-dp", lastOpened = 100, bookmarked = false)
        val recent = topic("algo-dp", lastOpened = 500, bookmarked = true)

        val merged = mergeProgress(mapOf(old.topicId to old), mapOf(recent.topicId to recent))

        assertTrue(merged.getValue("algo-dp").bookmarked)
    }

    @Test
    fun `a bookmark removed on the more recent device stays removed`() {
        // The reason bookmarked is not simply OR-ed: a rule that never loses a
        // bookmark is also a rule that can never remove one, and un-bookmarking
        // would silently stop working across devices.
        val old = topic("algo-dp", lastOpened = 100, bookmarked = true)
        val recent = topic("algo-dp", lastOpened = 500, bookmarked = false)

        val merged = mergeProgress(mapOf(old.topicId to old), mapOf(recent.topicId to recent))

        assertFalse(merged.getValue("algo-dp").bookmarked)
    }

    @Test
    fun `topics only one side has are kept from both`() {
        val mine = topic("os-paging")
        val theirs = topic("cd-parsing")

        val merged = mergeProgress(mapOf(mine.topicId to mine), mapOf(theirs.topicId to theirs))

        assertEquals(setOf("os-paging", "cd-parsing"), merged.keys)
    }

    @Test
    fun `merging with an empty side changes nothing`() {
        // Which is what a first sign-in looks like from both directions: a new
        // account with no server document, and a new device with no local one.
        val mine = mapOf("os-paging" to topic("os-paging", furthest = 0.4f))

        assertEquals(mine, mergeProgress(local = mine, remote = emptyMap()))
        assertEquals(mine, mergeProgress(local = emptyMap(), remote = mine))
    }

    @Test
    fun `merging is idempotent`() {
        // Sync runs on a schedule, so the same two documents get merged over
        // and over. A merge that kept changing its answer would push a new
        // revision every six hours for ever.
        val a = mapOf("os-paging" to topic("os-paging", furthest = 0.6f, lastOpened = 100))
        val b = mapOf("os-paging" to topic("os-paging", furthest = 0.3f, lastOpened = 200))

        val once = mergeProgress(a, b)
        assertEquals(once, mergeProgress(once, b))
        assertEquals(once, mergeProgress(once, once))
    }

    @Test
    fun `merging either way round gives the same answer`() {
        // Two devices must agree on the result, or they push conflicting
        // documents at each other indefinitely.
        val a = mapOf("os-paging" to topic("os-paging", furthest = 0.6f, lastOpened = 100, bookmarked = true))
        val b = mapOf("os-paging" to topic("os-paging", furthest = 0.3f, lastOpened = 200, bookmarked = false))

        assertEquals(mergeProgress(a, b), mergeProgress(b, a))
    }

    private fun topic(
        id: String,
        furthest: Float = 0f,
        lastOpened: Long = 0,
        bookmarked: Boolean = false,
    ) = TopicProgress(
        topicId = id,
        branchId = "cs",
        subjectId = id.substringBefore('-'),
        subjectName = "Subject",
        title = "Topic",
        path = "$id.html",
        lastOpenedEpochMs = lastOpened,
        furthest = furthest,
        bookmarked = bookmarked,
    )
}
