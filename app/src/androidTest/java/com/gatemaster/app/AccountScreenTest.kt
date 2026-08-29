package com.gatemaster.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gatemaster.app.core.data.auth.AuthState
import com.gatemaster.app.ui.account.AccountActions
import com.gatemaster.app.ui.account.AccountContent
import com.gatemaster.app.ui.account.AccountMode
import com.gatemaster.app.ui.account.AccountUiState
import com.gatemaster.app.ui.account.TAG_SUBMIT
import com.gatemaster.app.ui.account.TAG_SYNC_NOW
import com.gatemaster.app.ui.theme.GateMasterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The account screen, driven as a user would drive it.
 *
 * Every bug this screen has had was a UI-state bug -- a submit button under the
 * keyboard, an em dash written as two hyphens, an error that had to reach the
 * field it blamed. The JVM suite could not see any of them, because none of
 * them are logic. These can.
 *
 * They run against [AccountContent] rather than the whole screen, so the states
 * that are awkward to reach on a device -- no server configured, a rejected
 * field, a sync in flight -- are one line to set up.
 */
@RunWith(AndroidJUnit4::class)
class AccountScreenTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * The state is held in a MutableState rather than passed to setContent,
     * because setContent may only be called once per test -- and several of
     * these need to check that the screen changes when the state does.
     */
    private fun show(
        initial: AccountUiState,
        actions: AccountActions = AccountActions(),
    ): (AccountUiState) -> Unit {
        val holder = mutableStateOf(initial)
        compose.setContent {
            GateMasterTheme {
                // Read inside composition, so assigning to holder recomposes.
                AccountContent(state = holder.value, onBack = {}, actions = actions)
            }
        }
        return { next -> holder.value = next }
    }

    private fun signedOut(
        mode: AccountMode = AccountMode.SIGN_IN,
        email: String = "",
        password: String = "",
        error: String? = null,
        errorField: String? = null,
    ) = AccountUiState(
        auth = AuthState.SignedOut,
        mode = mode,
        email = email,
        password = password,
        error = error,
        errorField = errorField,
    )

    private fun signedIn(
        syncing: Boolean = false,
        syncMessage: String? = null,
    ) = AccountUiState(
        auth = AuthState.SignedIn("u1", "student@example.com", "Charan"),
        syncing = syncing,
        syncMessage = syncMessage,
    )

    // --- Signed out ---------------------------------------------------------

    @Test
    fun signedOutOffersBothVerbsAndSaysTheAccountIsOptional() {
        show(signedOut())

        compose.onNodeWithText("Create account").assertIsDisplayed()
        // The promise the whole design rests on. If it ever stops being true
        // the sentence has to go, and this failing is how anyone finds out.
        compose.onNodeWithText("Optional", substring = true).assertIsDisplayed()
    }

    @Test
    fun theNameFieldAppearsOnlyWhenCreatingAnAccount() {
        val update = show(signedOut(mode = AccountMode.SIGN_IN))
        compose.onNodeWithText("Name").assertDoesNotExist()

        update(signedOut(mode = AccountMode.CREATE))
        compose.onNodeWithText("Name").assertIsDisplayed()
    }

    @Test
    fun theMinimumPasswordLengthIsShownWhenCreatingAndNotWhenSigningIn() {
        // Otherwise the user learns the rule from a rejected round trip. On the
        // sign-in form it is hidden, because there the rule is whatever the
        // existing password already was, and a minimum would imply otherwise.
        val update = show(signedOut(mode = AccountMode.CREATE))
        compose.onNodeWithText("At least 8 characters").assertIsDisplayed()

        update(signedOut(mode = AccountMode.SIGN_IN))
        compose.onNodeWithText("At least 8 characters").assertDoesNotExist()
    }

    @Test
    fun submitIsDisabledUntilBothFieldsHaveSomethingInThem() {
        val update = show(signedOut())
        compose.onNodeWithTag(TAG_SUBMIT).assertIsNotEnabled()

        update(signedOut(email = "student@example.com", password = "correct-horse-battery"))
        compose.onNodeWithTag(TAG_SUBMIT).assertIsEnabled()
    }

    @Test
    fun theServersMessageIsShownAgainstTheFieldItBlames() {
        // Exactly what the device showed: the server rejects the address, and
        // the form has to put that message where the user is looking.
        show(
            signedOut(
                email = "not-an-email",
                password = "correct-horse-battery",
                error = "That does not look like an email address",
                errorField = "email",
            ),
        )

        compose.onNodeWithText("That does not look like an email address").assertIsDisplayed()
    }

    @Test
    fun typingReachesTheCallbacks() {
        val typed = mutableListOf<String>()
        show(signedOut(), AccountActions(onEmail = { typed += it }))

        compose.onNodeWithText("Email").performTextInput("a@b.com")
        assertTrue("expected the email callback to fire, got $typed", typed.isNotEmpty())
    }

    @Test
    fun submitReachesItsCallbackOnlyWhenItIsAllowedTo() {
        var submits = 0
        val update = show(signedOut(), AccountActions(onSubmit = { submits++ }))

        compose.onNodeWithTag(TAG_SUBMIT).performClick()
        assertEquals("an empty form must not submit", 0, submits)

        update(signedOut(email = "student@example.com", password = "correct-horse-battery"))
        compose.onNodeWithTag(TAG_SUBMIT).performClick()
        assertEquals(1, submits)
    }

    // --- Signed in ----------------------------------------------------------

    @Test
    fun signedInShowsWhoAndOffersSyncAndSignOut() {
        show(signedIn())

        compose.onNodeWithText("Charan").assertIsDisplayed()
        compose.onNodeWithText("student@example.com").assertIsDisplayed()
        compose.onNodeWithTag(TAG_SYNC_NOW).assertIsEnabled().assertHasClickAction()
        compose.onNodeWithText("Sign out").assertIsEnabled()
        // Signing in must never look like it took anything over.
        compose.onNodeWithText("leaves everything on this phone", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun theResultOfTheLastSyncIsReportedBack() {
        show(signedIn(syncMessage = "Synced: 2 sent, reading history saved."))
        compose.onNodeWithText("Synced: 2 sent, reading history saved.").assertIsDisplayed()
    }

    @Test
    fun syncCannotBeStartedAgainWhileOneIsRunning() {
        show(signedIn(syncing = true))
        compose.onNodeWithTag(TAG_SYNC_NOW).assertIsNotEnabled()
    }

    @Test
    fun syncNowReachesItsCallback() {
        var syncs = 0
        show(signedIn(), AccountActions(onSyncNow = { syncs++ }))

        compose.onNodeWithTag(TAG_SYNC_NOW).performClick()
        assertEquals(1, syncs)
    }

    // --- No server configured -----------------------------------------------

    @Test
    fun withNoServerTheScreenExplainsRatherThanOfferingAFormThatCannotWork() {
        show(AccountUiState(auth = AuthState.Unavailable))

        compose.onNodeWithText("Sync is not set up").assertIsDisplayed()
        // No form at all: there is nothing to sign in to yet.
        compose.onNodeWithText("Password").assertDoesNotExist()
    }

    @Test
    fun aReleasedAppNeverAsksTheUserToSupplyAServer() {
        // The bug this exists for: a published APK carries its own server, so
        // a field asking for a URL is a developer tool showing in the product.
        // Somebody who installs the app has nothing they could type here.
        show(AccountUiState(auth = AuthState.Unavailable, canChooseServer = false))

        compose.onNodeWithText("Sync server").assertDoesNotExist()
        compose.onNodeWithText("Set a server").assertDoesNotExist()
        compose.onNodeWithText("Point it at a server", substring = true).assertDoesNotExist()
    }

    @Test
    fun aDebugBuildStillOffersTheServerFieldSoItCanRunAgainstALocalOne() {
        show(AccountUiState(auth = AuthState.Unavailable, canChooseServer = true))

        compose.onNodeWithText("Sync server").assertIsDisplayed()
        compose.onNodeWithText("Set a server").assertIsDisplayed()
    }

    @Test
    fun theServerFieldIsHiddenWhileSignedInOnAReleasedApp() {
        show(signedIn())
        compose.onNodeWithText("Sync server").assertDoesNotExist()
    }

    @Test
    fun proseUsesRealPunctuation() {
        // A literal double hyphen shipped to the device once. No logic test can
        // see that; every screenshot can.
        show(AccountUiState(auth = AuthState.Unavailable))
        compose.onNodeWithText("--", substring = true).assertDoesNotExist()
    }
}
