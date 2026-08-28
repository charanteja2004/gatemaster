package com.gatemaster.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.data.auth.AuthRepository
import com.gatemaster.app.core.data.auth.AuthState
import com.gatemaster.app.core.data.sync.SyncManager
import com.gatemaster.app.core.data.sync.SyncOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
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
            val failure = when (state.mode) {
                AccountMode.SIGN_IN -> auth.login(state.email, state.password)
                AccountMode.CREATE -> auth.register(state.email, state.password, state.displayName)
            }
            _uiState.update {
                if (failure == null) {
                    // Clear the password on success. The state outlives this
                    // screen, and there is no reason for it to keep one.
                    it.copy(busy = false, password = "", error = null, errorField = null)
                } else {
                    it.copy(busy = false, error = failure.message, errorField = failure.field)
                }
            }
        }
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
