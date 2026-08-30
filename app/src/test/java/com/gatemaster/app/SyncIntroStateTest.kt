package com.gatemaster.app

import com.gatemaster.app.core.data.auth.AuthState
import com.gatemaster.app.ui.onboarding.SyncIntroUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The first-run offer of an account.
 *
 * It has to be skippable, and it has to disappear entirely on a build with no
 * sync server -- the same rule the Settings account row follows.
 */
class SyncIntroStateTest {

    @Test
    fun `nothing is drawn until the auth state is known`() {
        // Otherwise a build with no server flashes an offer for one frame and
        // then withdraws it.
        assertFalse(SyncIntroUiState().ready)
    }

    @Test
    fun `a build with no server has nothing to offer and stands aside`() {
        val state = SyncIntroUiState(auth = AuthState.Unavailable)
        assertTrue(state.ready)
        assertTrue(state.nothingToOffer)
    }

    @Test
    fun `a server exists and nobody is signed in, so the offer stands`() {
        val state = SyncIntroUiState(auth = AuthState.SignedOut)
        assertFalse(state.nothingToOffer)
        assertNull(state.signedInAs)
    }

    @Test
    fun `signing in is confirmed by name`() {
        val state = SyncIntroUiState(auth = AuthState.SignedIn("u", "a@b.com", "Charan"))
        assertEquals("Charan", state.signedInAs)
    }

    @Test
    fun `an account with no display name falls back to its email`() {
        val state = SyncIntroUiState(auth = AuthState.SignedIn("u", "a@b.com", ""))
        assertEquals("a@b.com", state.signedInAs)
    }
}
