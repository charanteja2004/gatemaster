package com.gatemaster.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.ProgressRepository
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.data.db.ProgressTotals
import com.gatemaster.app.core.data.db.ScorePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One subject's record across every attempt. */
data class SubjectProgress(
    val subjectId: String,
    val name: String,
    val attempted: Int,
    val correct: Int,
    val accuracy: Float,
)

/** One topic worth going back to. */
data class TopicProgress(
    val topicId: String,
    val title: String,
    val subjectName: String?,
    val attempted: Int,
    val correct: Int,
    val accuracy: Float,
)

data class ProgressUiState(
    val isLoading: Boolean = true,
    val totals: ProgressTotals = ProgressTotals.EMPTY,
    val trend: List<ScorePoint> = emptyList(),
    val subjects: List<SubjectProgress> = emptyList(),
    val weakTopics: List<TopicProgress> = emptyList(),
) {
    val hasHistory: Boolean get() = totals.attempts > 0

    /** Enough points for a trend to mean anything. */
    val hasTrend: Boolean get() = trend.size >= 2

    val best: ScorePoint? get() = trend.maxByOrNull { it.percent }
}

/**
 * Turns the attempt history into the two questions a scorecard cannot answer:
 * which subjects are costing marks, and whether that is improving.
 *
 * The aggregates arrive already computed from SQL; this only puts names to the
 * ids, which the database has no reason to know.
 */
class ProgressViewModel(
    private val progress: ProgressRepository,
    private val content: ContentRepository,
    private val preferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val branchId = preferences.branchId.first()
            val subjects = content.subjects(branchId)
            val subjectNames = subjects.associate { it.id to it.name }
            val topicTitles = subjects
                .flatMap { subject -> subject.topics.map { it.id to it.title } }
                .toMap()

            combine(
                progress.totals(),
                progress.scoreTrend(),
                progress.subjectAccuracy(),
                progress.weakestTopics(),
            ) { totals, trend, bySubject, weakest ->
                ProgressUiState(
                    isLoading = false,
                    totals = totals,
                    trend = trend,
                    subjects = bySubject.map { row ->
                        SubjectProgress(
                            subjectId = row.subjectId,
                            name = subjectNames[row.subjectId] ?: row.subjectId,
                            attempted = row.attempted,
                            correct = row.correct,
                            accuracy = row.accuracy,
                        )
                    },
                    weakTopics = weakest.map { row ->
                        TopicProgress(
                            topicId = row.topicId,
                            // A generated question stores the topic id; fall
                            // back to it when the index no longer has a title.
                            title = topicTitles[row.topicId] ?: row.topicId,
                            subjectName = row.subjectId?.let { subjectNames[it] },
                            attempted = row.attempted,
                            correct = row.correct,
                            accuracy = row.accuracy,
                        )
                    },
                )
            }.collect { state -> _uiState.update { state } }
        }
    }
}
