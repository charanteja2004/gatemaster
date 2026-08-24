package com.gatemaster.app.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.AttemptRecord
import com.gatemaster.app.core.data.TestRepository
import com.gatemaster.app.core.model.TestSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TestEntry(
    val summary: TestSummary,
    val inProgress: Boolean,
)

data class TestListUiState(
    val isLoading: Boolean = true,
    val tests: List<TestEntry> = emptyList(),
    val history: List<AttemptRecord> = emptyList(),
    val resumePromptTestId: String? = null,
)

class TestListViewModel(
    private val repository: TestRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestListUiState())
    val uiState: StateFlow<TestListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Re-read on every return to the screen so history stays current. */
    fun refresh() {
        viewModelScope.launch {
            val summaries = repository.catalogue()
            val entries = summaries.map { summary ->
                TestEntry(summary, repository.hasAttemptInProgress(summary.id))
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    tests = entries,
                    history = repository.history(),
                )
            }
        }
    }

    fun askResumeOrRestart(testId: String) =
        _uiState.update { it.copy(resumePromptTestId = testId) }

    fun dismissResumePrompt() = _uiState.update { it.copy(resumePromptTestId = null) }
}
