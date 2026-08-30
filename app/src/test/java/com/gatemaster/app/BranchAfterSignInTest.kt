package com.gatemaster.app

import com.gatemaster.app.core.data.TopicProgress
import com.gatemaster.app.core.data.branchAfterSignIn
import com.gatemaster.app.core.data.dominantBranch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Which paper an install lands on after signing in.
 *
 * The gap this closes: sign in on a new phone, a term of reading downloads,
 * and home shows none of it because the paper was picked from a list a minute
 * earlier and the history belongs to a different one.
 */
class BranchAfterSignInTest {

    private fun topic(
        id: String,
        branch: String,
        openedAt: Long = 1_000L,
    ) = TopicProgress(
        topicId = id,
        branchId = branch,
        subjectId = "s",
        subjectName = "Subject",
        title = id,
        path = "$id.html",
        lastOpenedEpochMs = openedAt,
    )

    private fun progress(vararg topics: TopicProgress) = topics.associateBy { it.topicId }

    // -- dominantBranch ------------------------------------------------------

    @Test
    fun `no progress has no dominant paper`() {
        assertNull(emptyMap<String, TopicProgress>().dominantBranch())
    }

    @Test
    fun `a topic that was never opened does not vote`() {
        // Entries exist for anything the reader touched; an unopened one is a
        // bookmark or a stub, not evidence of which paper they are sitting.
        val state = progress(topic("a", "cs", openedAt = 0))
        assertNull(state.dominantBranch())
    }

    @Test
    fun `the paper with the most opened topics wins`() {
        val state = progress(
            topic("a", "cs"),
            topic("b", "cs"),
            topic("c", "me"),
        )
        assertEquals("cs", state.dominantBranch())
    }

    @Test
    fun `one stray tap does not outvote a term of reading`() {
        val state = progress(
            topic("a", "ec"),
            topic("b", "ec"),
            topic("c", "ec"),
            topic("stray", "cs", openedAt = 9_999L),
        )
        assertEquals("ec", state.dominantBranch())
    }

    @Test
    fun `a tie breaks towards whatever was read most recently`() {
        val state = progress(
            topic("a", "cs", openedAt = 100L),
            topic("b", "me", openedAt = 200L),
        )
        assertEquals("me", state.dominantBranch())
    }

    // -- branchAfterSignIn ---------------------------------------------------

    @Test
    fun `a fresh install adopts the paper its downloaded history is in`() {
        val synced = progress(topic("a", "ec"), topic("b", "ec"))
        assertEquals(
            "ec",
            branchAfterSignIn(current = "cs", hadLocalProgress = false, synced = synced),
        )
    }

    @Test
    fun `reading already on this phone is never overruled`() {
        // The strongest rule here. Someone studying ME on this phone who signs
        // into an account with more CS history keeps ME: their own reading is
        // better evidence than the account's.
        val synced = progress(topic("a", "cs"), topic("b", "cs"))
        assertNull(branchAfterSignIn(current = "me", hadLocalProgress = true, synced = synced))
    }

    @Test
    fun `an account with no history changes nothing`() {
        assertNull(
            branchAfterSignIn(current = "cs", hadLocalProgress = false, synced = emptyMap()),
        )
    }

    @Test
    fun `already on the right paper is left alone`() {
        val synced = progress(topic("a", "cs"))
        assertNull(branchAfterSignIn(current = "cs", hadLocalProgress = false, synced = synced))
    }

    @Test
    fun `a brand new account on a brand new phone changes nothing`() {
        // Registering rather than signing in: nothing local, nothing remote.
        assertNull(
            branchAfterSignIn(current = "cs", hadLocalProgress = false, synced = emptyMap()),
        )
    }
}
