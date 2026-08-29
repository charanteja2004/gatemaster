package com.gatemaster.app.ui.branch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.model.Branch
import com.gatemaster.app.core.model.ExamCalendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BranchPickerUiState(
    val isLoading: Boolean = true,
    val all: List<Branch> = emptyList(),
    val query: String = "",
    val selectedId: String? = null,
    /**
     * The exam the papers are for. Read rather than hard-coded, because a
     * literal year here disagreed with the countdown on home for eleven months
     * out of every twelve.
     */
    val examYear: Int = ExamCalendar.nextExamYear(),
) {
    /**
     * Papers matching the query. Code matches rank first so typing "ME" puts
     * Mechanical Engineering above every paper whose name merely contains "me".
     */
    val visible: List<Branch>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return all
            return all
                .mapNotNull { branch ->
                    val rank = when {
                        branch.code.equals(q, ignoreCase = true) -> 0
                        branch.code.startsWith(q, ignoreCase = true) -> 1
                        branch.name.startsWith(q, ignoreCase = true) -> 2
                        branch.name.contains(q, ignoreCase = true) -> 3
                        branch.shortName.contains(q, ignoreCase = true) -> 4
                        else -> null
                    }
                    rank?.let { it to branch }
                }
                .sortedWith(compareBy({ it.first }, { it.second.order }))
                .map { it.second }
        }
}

class BranchPickerViewModel(
    private val repository: ContentRepository,
    private val preferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BranchPickerUiState())
    val uiState: StateFlow<BranchPickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val branches = repository.branches()
            val current = preferences.branchId.first()
            _uiState.update {
                it.copy(isLoading = false, all = branches, selectedId = current)
            }
        }
    }

    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query) }

    /**
     * Persists the choice, then reports back so the caller can navigate.
     *
     * Navigating first would pop this destination and cancel viewModelScope
     * mid-write, silently losing the selection — which is exactly what happened
     * the first time this screen was wired up.
     */
    fun select(branchId: String, onSaved: () -> Unit) {
        _uiState.update { it.copy(selectedId = branchId) }
        viewModelScope.launch {
            preferences.setBranch(branchId)
            onSaved()
        }
    }
}
