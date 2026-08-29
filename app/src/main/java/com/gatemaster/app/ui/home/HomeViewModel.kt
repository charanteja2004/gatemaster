package com.gatemaster.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.StudyProgressRepository
import com.gatemaster.app.core.data.TopicProgress
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.model.ExamCalendar
import com.gatemaster.app.core.data.bookmarks
import com.gatemaster.app.core.data.continueReading
import com.gatemaster.app.core.data.readCount
import com.gatemaster.app.core.data.readCountForBranch
import com.gatemaster.app.core.model.Branch
import com.gatemaster.app.core.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class HomeUiState(
    val isLoading: Boolean = true,
    val branch: Branch? = null,
    val paperCount: Int = 0,
    val daysToExam: Long = 0,
    val examYear: Int = 0,
    val errorMessage: String? = null,
    val continueReading: TopicProgress? = null,
    val bookmarkCount: Int = 0,
    val readCount: Int = 0,
    /** Read count per subject id, for the progress rings in the lists. */
    val readBySubject: Map<String, Int> = emptyMap(),
) {
    val subjects: List<Subject> get() = branch?.subjects.orEmpty()

    /**
     * The four heaviest subjects. Home is a dashboard; the full list lives in
     * the Study tab, and showing eleven rows here buries everything else.
     */
    val topSubjects: List<Subject>
        get() = subjects.sortedByDescending { it.weightage }.take(4)

    val totalItems: Int get() = branch?.totalTopics ?: 0
}

class HomeViewModel(
    private val repository: ContentRepository,
    private val preferences: UserPreferences,
    private val studyProgress: StudyProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            studyProgress.load()
            combine(preferences.branchId, studyProgress.progress) { branchId, progress ->
                branchId to progress
            }.collect { (branchId, progress) ->
                load(branchId, progress)
            }
        }
    }

    fun retry() {
        viewModelScope.launch {
            load(_uiState.value.branch?.id ?: "cs", studyProgress.progress.value)
        }
    }

    private suspend fun load(branchId: String, progress: Map<String, TopicProgress>) {
        repository.index()
            .onSuccess { index ->
                val branch = index.branch(branchId) ?: index.branches.firstOrNull()
                val papers = branch?.let { index.papersFor(it) }.orEmpty()
                val exam = ExamCalendar.nextExamDate()
                val actualBranchId = branch?.id ?: branchId

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        branch = branch,
                        paperCount = papers.size,
                        daysToExam = ChronoUnit.DAYS.between(LocalDate.now(), exam)
                            .coerceAtLeast(0),
                        examYear = exam.year,
                        continueReading = progress.continueReading(actualBranchId),
                        bookmarkCount = progress.bookmarks(actualBranchId).size,
                        readCount = progress.readCountForBranch(actualBranchId),
                        readBySubject = branch?.subjects.orEmpty()
                            .associate { s -> s.id to progress.readCount(s.id) },
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Study material could not be loaded. " +
                            (error.message ?: "Try reinstalling the app."),
                    )
                }
            }
    }

}
