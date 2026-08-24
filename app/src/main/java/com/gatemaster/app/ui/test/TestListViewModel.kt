package com.gatemaster.app.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.AttemptRecord
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.TestRepository
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.model.TestSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TestEntry(
    val summary: TestSummary,
    val inProgress: Boolean,
)

/** A subject that has enough questions to practise. */
data class PracticeEntry(
    val subjectId: String,
    val subjectName: String,
    val shortName: String,
    val questionCount: Int,
    val topicCount: Int,
)

data class TestListUiState(
    val isLoading: Boolean = true,
    val tests: List<TestEntry> = emptyList(),
    val practice: List<PracticeEntry> = emptyList(),
    val history: List<AttemptRecord> = emptyList(),
    val resumePromptTestId: String? = null,
)

class TestListViewModel(
    private val repository: TestRepository,
    private val contentRepository: ContentRepository,
    private val preferences: UserPreferences,
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

            // Practice sets are listed here rather than hidden behind a bolt on
            // a topic row: this is where anyone goes looking for a test, and a
            // feature nobody can find may as well not exist.
            val branchId = preferences.branchId.first()
            val practice = contentRepository.subjects(branchId).mapNotNull { subject ->
                val count = repository.questionCount(subject.id)
                if (count < MIN_SUBJECT_QUESTIONS) return@mapNotNull null
                PracticeEntry(
                    subjectId = subject.id,
                    subjectName = subject.name,
                    shortName = subject.shortName,
                    questionCount = count,
                    topicCount = repository.topicsWithQuestions(subject.id).size,
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    tests = entries,
                    practice = practice,
                    history = repository.history(),
                )
            }
        }
    }

    fun askResumeOrRestart(testId: String) =
        _uiState.update { it.copy(resumePromptTestId = testId) }

    fun dismissResumePrompt() = _uiState.update { it.copy(resumePromptTestId = null) }

    private companion object {
        const val MIN_SUBJECT_QUESTIONS = 5
    }
}
