package com.gatemaster.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.model.Branch
import com.gatemaster.app.core.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

data class HomeUiState(
    val isLoading: Boolean = true,
    val branch: Branch? = null,
    val paperCount: Int = 0,
    val latestPaperYear: Int? = null,
    val daysToExam: Long = 0,
    val examYear: Int = 0,
    val errorMessage: String? = null,
) {
    val subjects: List<Subject> get() = branch?.subjects.orEmpty()
    val totalItems: Int get() = branch?.totalTopics ?: 0
    val subjectsWithNotes: Int get() = branch?.subjectsWithNotes ?: 0
}

class HomeViewModel(
    private val repository: ContentRepository,
    private val preferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.branchId.collect { branchId -> load(branchId) }
        }
    }

    fun retry() {
        viewModelScope.launch {
            load(_uiState.value.branch?.id ?: "cs")
        }
    }

    private suspend fun load(branchId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        repository.index()
            .onSuccess { index ->
                val branch = index.branch(branchId) ?: index.branches.firstOrNull()
                val papers = branch?.let { index.papersFor(it) }.orEmpty()
                val exam = nextExam()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        branch = branch,
                        paperCount = papers.size,
                        latestPaperYear = papers.maxOfOrNull { p -> p.year },
                        daysToExam = ChronoUnit.DAYS.between(LocalDate.now(), exam)
                            .coerceAtLeast(0),
                        examYear = exam.year,
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

    /**
     * GATE runs on the first two weekends of February. The first Saturday of
     * February is close enough for a countdown, and rolls over to next year
     * once this year's exam has passed.
     */
    private fun nextExam(): LocalDate {
        val today = LocalDate.now()
        var candidate = firstSaturdayOfFebruary(today.year)
        if (!candidate.isAfter(today)) {
            candidate = firstSaturdayOfFebruary(today.year + 1)
        }
        return candidate
    }

    private fun firstSaturdayOfFebruary(year: Int): LocalDate {
        var date = LocalDate.of(year, Month.FEBRUARY, 1)
        while (date.dayOfWeek.value != 6) {
            date = date.plusDays(1)
        }
        return date
    }
}
