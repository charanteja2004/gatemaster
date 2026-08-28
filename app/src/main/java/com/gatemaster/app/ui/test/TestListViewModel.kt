package com.gatemaster.app.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.ProgressRepository
import com.gatemaster.app.core.data.TestRepository
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.data.db.AttemptEntity
import com.gatemaster.app.core.model.PracticeSpec
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
    val history: List<AttemptEntity> = emptyList(),
    val resumePromptTestId: String? = null,
    /** Subjects that can go into a mixed paper, in paper order. */
    val mixSubjects: List<PracticeEntry> = emptyList(),
    val selectedMix: Set<String> = emptySet(),
    val isChoosingMix: Boolean = false,
    /** How many topics the attempt history has anything to say about. */
    val practisedTopics: Int = 0,
) {
    /**
     * Recommendations need something to recommend from.
     *
     * Three topics, not one: with a single topic in the history the "set drawn
     * from your weakest topics" is that one topic, which is a topic practice
     * set wearing a different name.
     */
    val canRecommend: Boolean get() = practisedTopics >= 3

    val recommendedId: String get() = PracticeSpec.adaptive().id

    /** A mix needs at least two subjects to be a mix. */
    val canMix: Boolean get() = mixSubjects.size >= 2

    val mixedQuestionCount: Int get() = mixSubjects.sumOf { it.questionCount }

    /** The whole-paper mix: everything that has questions. */
    val everythingMixId: String get() = PracticeSpec.mixed().id

    /** Null until enough subjects are ticked for the choice to mean anything. */
    val customMixId: String?
        get() = mixSubjects.map { it.subjectId }
            .filter { it in selectedMix }
            .takeIf { it.size >= 2 }
            ?.let { PracticeSpec.mixed(it).id }
}

class TestListViewModel(
    private val repository: TestRepository,
    private val contentRepository: ContentRepository,
    private val preferences: UserPreferences,
    private val progress: ProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestListUiState())
    val uiState: StateFlow<TestListUiState> = _uiState.asStateFlow()

    init {
        refresh()
        // History comes from the database now, so it updates itself when an
        // attempt is recorded rather than waiting for the screen to be
        // revisited.
        viewModelScope.launch {
            progress.recentAttempts().collect { attempts ->
                _uiState.update { it.copy(history = attempts) }
            }
        }
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
                    // The same subjects, offered as ingredients for a mixed
                    // paper rather than as one test each.
                    mixSubjects = practice,
                    practisedTopics = progress.topicHistory().size,
                )
            }
        }
    }

    fun askResumeOrRestart(testId: String) =
        _uiState.update { it.copy(resumePromptTestId = testId) }

    fun dismissResumePrompt() = _uiState.update { it.copy(resumePromptTestId = null) }

    // -- building a mixed paper -----------------------------------------------

    fun chooseMix() = _uiState.update {
        // Everything starts ticked: the common case is dropping the one or two
        // subjects you have not studied yet, not building a mix from nothing.
        it.copy(
            isChoosingMix = true,
            selectedMix = it.mixSubjects.map(PracticeEntry::subjectId).toSet(),
        )
    }

    fun dismissMixPicker() = _uiState.update { it.copy(isChoosingMix = false) }

    fun toggleMixSubject(subjectId: String) = _uiState.update { state ->
        val selected = state.selectedMix
        state.copy(
            selectedMix = if (subjectId in selected) {
                selected - subjectId
            } else {
                selected + subjectId
            },
        )
    }

    private companion object {
        const val MIN_SUBJECT_QUESTIONS = 5
    }
}
