package com.gatemaster.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.StudyProgressRepository
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.data.branchAfterSignIn
import com.gatemaster.app.core.data.auth.AuthRepository
import com.gatemaster.app.core.data.auth.AuthState
import com.gatemaster.app.core.data.sync.SyncManager
import com.gatemaster.app.core.data.sync.SyncOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Sign in to an existing account, or make a new one. Same form, two verbs. */
enum class AccountMode { SIGN_IN, CREATE }

data class AccountUiState(
    val auth: AuthState = AuthState.SignedOut,
    val mode: AccountMode = AccountMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val serverUrl: String = "",
    val editingServer: Boolean = false,
    /**
     * Whether to offer the sync-server field at all.
     *
     * False in a released APK, which already carries the server it is meant to
     * talk to. Asking a student to supply a URL is asking them to deploy a Ktor
     * service to sync their reading progress, which is not a feature -- it is a
     * developer tool that was showing in the product.
     */
    val canChooseServer: Boolean = false,
    val busy: Boolean = false,
    val syncing: Boolean = false,
    /** What the last manual sync did, in one line. Cleared on the next action. */
    val syncMessage: String? = null,
    val error: String? = null,
    /** "email" or "password", so the form can mark the field at fault. */
    val errorField: String? = null,
) {
    /**
     * Enough filled in to be worth a round trip.
     *
     * Deliberately loose -- the server is the authority on whether a password
     * is long enough, and a client that duplicates that rule gets to be wrong
     * about it after the server's changes.
     */
    val canSubmit: Boolean
        get() = !busy && email.isNotBlank() && password.isNotBlank()
}

class AccountViewModel(
    private val auth: AuthRepository,
    private val preferences: UserPreferences,
    private val sync: SyncManager,
    private val studyProgress: StudyProgressRepository,
    private val content: ContentRepository,
    /** True only in a debug build; see [AccountUiState.canChooseServer]. */
    canChooseServer: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState(canChooseServer = canChooseServer))
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            auth.state.collect { state -> _uiState.update { it.copy(auth = state) } }
        }
        viewModelScope.launch {
            preferences.syncBaseUrlOverride.collect { url ->
                _uiState.update { state ->
                    // Only adopt the stored value while the field is not being
                    // edited, or every keystroke would be overwritten by the
                    // last saved value coming back through the flow.
                    if (state.editingServer) state else state.copy(serverUrl = url)
                }
            }
        }
    }

    fun setMode(mode: AccountMode) = _uiState.update {
        it.copy(mode = mode, error = null, errorField = null)
    }

    fun setEmail(value: String) = _uiState.update {
        it.copy(email = value, error = null, errorField = null)
    }

    fun setPassword(value: String) = _uiState.update {
        it.copy(password = value, error = null, errorField = null)
    }

    fun setDisplayName(value: String) = _uiState.update { it.copy(displayName = value) }

    fun startEditingServer() = _uiState.update { it.copy(editingServer = true) }

    fun setServerUrl(value: String) = _uiState.update { it.copy(serverUrl = value) }

    fun saveServerUrl() {
        val url = _uiState.value.serverUrl
        _uiState.update { it.copy(editingServer = false, error = null) }
        viewModelScope.launch { preferences.setSyncBaseUrlOverride(url) }
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        _uiState.update { it.copy(busy = true, error = null, errorField = null) }
        viewModelScope.launch {
            // Read before anything is downloaded, and after a load() so an
            // unopened repository does not read as an empty one. Once the sync
            // below has merged an account's history in there is no way left to
            // tell whether this phone had any reading of its own.
            studyProgress.load()
            val hadLocalProgress = studyProgress.progress.value.isNotEmpty()

            val failure = when (state.mode) {
                AccountMode.SIGN_IN -> auth.login(state.email, state.password)
                AccountMode.CREATE -> auth.register(state.email, state.password, state.displayName)
            }

            if (failure != null) {
                _uiState.update {
                    it.copy(busy = false, error = failure.message, errorField = failure.field)
                }
                return@launch
            }

            _uiState.update {
                // Clear the password on success. The state outlives this
                // screen, and there is no reason for it to keep one.
                it.copy(busy = false, password = "", error = null, errorField = null, syncing = true)
            }

            // Sync immediately rather than waiting for the worker. Signing in
            // and then seeing none of your own reading for six hours is
            // indistinguishable from it not having worked.
            sync.sync()
            val message = adoptBranchFromAccount(hadLocalProgress)
            _uiState.update { it.copy(syncing = false, syncMessage = message) }
        }
    }

    /**
     * Moves this install onto the paper the downloaded history belongs to.
     *
     * Signing in on a new phone pulls down a term of reading and then shows a
     * home screen with none of it on it, because the paper was picked from a
     * list a minute earlier and the progress belongs to a different one. The
     * decision itself is [branchAfterSignIn], which declines to touch anything
     * when this phone already had reading of its own.
     */
    private suspend fun adoptBranchFromAccount(hadLocalProgress: Boolean): String? {
        val target = branchAfterSignIn(
            current = preferences.branchId.first(),
            hadLocalProgress = hadLocalProgress,
            synced = studyProgress.progress.value,
        ) ?: return null

        preferences.setBranch(target)
        val name = content.branch(target)?.name ?: return null
        return "Switched to $name, which is where your saved progress is."
    }

    /**
     * Syncs now, in the foreground, and says what happened.
     *
     * The background worker does the same work on a schedule. This exists
     * because "did my progress actually reach the server?" is a question a
     * user will ask, and an answer that arrives in six hours is not one.
     */
    fun syncNow() {
        if (_uiState.value.syncing) return
        _uiState.update { it.copy(syncing = true, syncMessage = null) }
        viewModelScope.launch {
            val message = when (val outcome = sync.sync()) {
                is SyncOutcome.Success -> describe(outcome)
                SyncOutcome.NothingToDo -> "Nothing to sync."
                SyncOutcome.SignedOut -> "Your session expired. Sign in again."
                is SyncOutcome.Retry -> "Could not reach the server. It will retry on its own."
            }
            _uiState.update { it.copy(syncing = false, syncMessage = message) }
        }
    }

    private fun describe(outcome: SyncOutcome.Success): String {
        val parts = buildList {
            if (outcome.attemptsUploaded > 0) add("${outcome.attemptsUploaded} sent")
            if (outcome.attemptsDownloaded > 0) add("${outcome.attemptsDownloaded} received")
            if (outcome.progressPushed) add("reading history saved")
        }
        return if (parts.isEmpty()) "Already up to date." else "Synced: " + parts.joinToString(", ") + "."
    }

    fun signOut() {
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            auth.logout()
            _uiState.update {
                it.copy(busy = false, email = "", password = "", displayName = "")
            }
        }
    }
}
