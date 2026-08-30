package com.gatemaster.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.auth.AuthRepository
import com.gatemaster.app.core.data.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncIntroUiState(
    /**
     * Null until the first auth state arrives. The screen draws nothing in
     * that moment: this step is skipped outright on a build with no server,
     * and showing an offer for half a frame before withdrawing it is worse
     * than showing nothing at all.
     */
    val auth: AuthState? = null,
) {
    /** Set once signing in has happened, so the step can confirm and move on. */
    val signedInAs: String?
        get() = (auth as? AuthState.SignedIn)
            ?.let { it.displayName.ifBlank { it.email } }

    /**
     * True when there is no sync server for this install. There is no account
     * to offer, so the step stands aside rather than advertising something
     * that cannot be done -- the same reasoning that hides the Settings row.
     */
    val nothingToOffer: Boolean get() = auth is AuthState.Unavailable

    val ready: Boolean get() = auth != null
}

/** Backs the first-run offer of an account. Reads auth state; changes nothing. */
class SyncIntroViewModel(auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncIntroUiState())
    val uiState: StateFlow<SyncIntroUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            auth.state.collect { state -> _uiState.update { it.copy(auth = state) } }
        }
    }
}
