package com.gatemaster.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.data.auth.AuthRepository
import com.gatemaster.app.core.data.auth.AuthState
import com.gatemaster.app.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val branchName: String = "",
    val branchCode: String = "",
    val articleCount: Int = 0,
    val paperCount: Int = 0,
    val auth: AuthState = AuthState.SignedOut,
) {
    val accountTitle: String
        get() = when (val state = auth) {
            is AuthState.SignedIn -> state.displayName.ifBlank { state.email }
            else -> "Not signed in"
        }

    val accountSubtitle: String
        get() = when (auth) {
            is AuthState.SignedIn -> "Progress syncs across your devices"
            // Not "no server set", which reads as a step the user should take.
            // There is nothing for them to do about it.
            AuthState.Unavailable -> "Sync is not available in this build"
            AuthState.SignedOut -> "Optional — sync progress across devices"
        }
}

class SettingsViewModel(
    private val repository: ContentRepository,
    private val preferences: UserPreferences,
    auth: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(preferences.themeMode, preferences.branchId) { theme, branchId ->
                theme to branchId
            }.collect { (theme, branchId) ->
                val branch = repository.branch(branchId)
                val papers = repository.papers(branchId)
                _uiState.update {
                    it.copy(
                        themeMode = theme,
                        branchName = branch?.name.orEmpty(),
                        branchCode = branch?.code.orEmpty(),
                        articleCount = branch?.totalTopics ?: 0,
                        paperCount = papers.size,
                    )
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            auth.state.collect { state -> _uiState.update { it.copy(auth = state) } }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }
}
