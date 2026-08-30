package com.gatemaster.app

import com.gatemaster.app.core.data.auth.AuthState
import com.gatemaster.app.ui.settings.SettingsUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Whether Settings offers an account row at all.
 *
 * A released build with no sync server has nothing behind that row: no account
 * to create, nothing to configure, and no action the reader can take. It was
 * showing anyway, and tapping it led to a screen whose only advice was aimed at
 * whoever built the app.
 */
class SettingsAccountVisibilityTest {

    @Test
    fun `a released build with no server does not offer an account row`() {
        val state = SettingsUiState(auth = AuthState.Unavailable, canChooseServer = false)
        assertFalse(state.showAccount)
    }

    @Test
    fun `a debug build still offers it, so a server can be pointed at`() {
        val state = SettingsUiState(auth = AuthState.Unavailable, canChooseServer = true)
        assertTrue(state.showAccount)
    }

    @Test
    fun `once a server exists the row is offered to everyone`() {
        assertTrue(SettingsUiState(auth = AuthState.SignedOut).showAccount)
        assertTrue(
            SettingsUiState(auth = AuthState.SignedIn("u", "a@b.com", "A")).showAccount,
        )
    }

    @Test
    fun `a signed-in user keeps the row even if the server setting is cleared`() {
        // AuthRepository reports SignedIn ahead of Unavailable for exactly this
        // reason: a session that outlives its configuration is still a session,
        // and hiding the row would strand the user with no way to sign out.
        val state = SettingsUiState(
            auth = AuthState.SignedIn("u", "a@b.com", "A"),
            canChooseServer = false,
        )
        assertTrue(state.showAccount)
    }
}
