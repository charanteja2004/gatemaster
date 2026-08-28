package com.gatemaster.app.ui.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gatemaster.app.core.data.ProgressRepository
import com.gatemaster.app.core.data.TestRepository
import com.gatemaster.app.core.model.Attempt
import com.gatemaster.app.core.model.AnswerState
import com.gatemaster.app.core.model.MockTest
import com.gatemaster.app.core.model.Question
import com.gatemaster.app.core.model.QuestionStatus
import com.gatemaster.app.core.model.QuestionType
import com.gatemaster.app.core.model.Scorecard
import com.gatemaster.app.core.model.scoreAttempt
import com.gatemaster.app.navigation.TestPlayerRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A labelled run of questions in the palette. */
data class PaletteSection(val name: String, val questions: List<Question>)

data class TestPlayerUiState(
    val isLoading: Boolean = true,
    val test: MockTest? = null,
    val attempt: Attempt? = null,
    val remainingMs: Long = 0,
    val showPalette: Boolean = false,
    val showSubmitConfirm: Boolean = false,
    val scorecard: Scorecard? = null,
    val errorMessage: String? = null,
) {
    val questions: List<Question> get() = test?.orderedQuestions.orEmpty()

    /**
     * Questions grouped by section, in paper order. A mixed paper carries one
     * section per subject, and picking "the Databases questions" out of a flat
     * grid of thirty numbers is not something anyone should do by counting.
     */
    val paletteSections: List<PaletteSection>
        get() = test?.let { paper ->
            paper.sections.mapNotNull { section ->
                section.questionIds.mapNotNull(paper::question)
                    .takeIf { it.isNotEmpty() }
                    ?.let { PaletteSection(section.name, it) }
            }
        }.orEmpty()

    /** One section is the whole paper, and naming it adds nothing. */
    val showsSectionHeadings: Boolean get() = paletteSections.size > 1

    val currentIndex: Int get() = attempt?.currentIndex ?: 0

    val currentQuestion: Question? get() = questions.getOrNull(currentIndex)

    val currentAnswer: AnswerState?
        get() = currentQuestion?.let { attempt?.answerFor(it.id) }

    val isFirst: Boolean get() = currentIndex == 0
    val isLast: Boolean get() = currentIndex >= questions.lastIndex

    fun statusOf(question: Question): QuestionStatus =
        attempt?.answerFor(question.id)?.status ?: QuestionStatus.NOT_VISITED

    val answeredCount: Int
        get() = attempt?.answers?.values?.count { it.status.isAnswered } ?: 0

    val markedCount: Int
        get() = attempt?.answers?.values?.count { it.status.isMarked } ?: 0
}

/**
 * Drives one sitting of a test.
 *
 * Navigation is completely independent of correctness — you can move on from
 * any question, answered or not, right or wrong. The previous engine advanced
 * its index only inside the "correct answer" branch, so a wrong answer left
 * the user stuck on the same question with no way forward, and answering the
 * last question correctly ran off the end of the list.
 */
class TestPlayerViewModel(
    private val repository: TestRepository,
    private val progress: ProgressRepository,
    savedStateHandle: SavedStateHandle,
    /** Called once a sitting is recorded, so it reaches the account that owns it. */
    private val requestSync: () -> Unit = {},
) : ViewModel() {

    private val testId: String = savedStateHandle.toRoute<TestPlayerRoute>().testId
    private val restart: Boolean = savedStateHandle.toRoute<TestPlayerRoute>().restart

    private val _uiState = MutableStateFlow(TestPlayerUiState())
    val uiState: StateFlow<TestPlayerUiState> = _uiState.asStateFlow()

    /** Wall-clock instant the timer last started running; null while paused. */
    private var runningSinceMs: Long? = null
    private var tickerJob: Job? = null
    private var questionEnteredAtMs: Long = System.currentTimeMillis()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        repository.loadTest(testId)
            .onSuccess { test ->
                val saved = if (restart) null else repository.loadAttempt(testId)
                if (restart) repository.clearAttempt(testId)

                val attempt = saved?.takeIf { !it.isSubmitted } ?: Attempt(
                    testId = test.id,
                    startedAtEpochMs = System.currentTimeMillis(),
                    durationMinutes = test.durationMinutes,
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        test = test,
                        attempt = markVisited(attempt, test, attempt.currentIndex),
                        remainingMs = attempt.remainingMs,
                    )
                }
                resumeTimer()
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "This test could not be opened. ${error.message.orEmpty()}",
                    )
                }
            }
    }

    // -- timer ---------------------------------------------------------------

    /** Called when the screen becomes visible. Safe to call repeatedly. */
    fun resumeTimer() {
        val state = _uiState.value
        if (state.attempt == null || state.scorecard != null) return
        if (runningSinceMs != null) return

        runningSinceMs = System.currentTimeMillis()
        questionEnteredAtMs = System.currentTimeMillis()
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val attempt = _uiState.value.attempt ?: break
                val remaining = (attempt.totalDurationMs - liveElapsed(attempt)).coerceAtLeast(0)
                _uiState.update { it.copy(remainingMs = remaining) }
                if (remaining == 0L) {
                    submit(auto = true)
                    break
                }
                delay(TICK_MS)
            }
        }
    }

    /**
     * Banks elapsed time and persists. Called when the screen is backgrounded,
     * so a practice attempt does not silently burn its clock while the app is
     * closed.
     */
    fun pauseTimer() {
        tickerJob?.cancel()
        tickerJob = null
        val started = runningSinceMs ?: return
        runningSinceMs = null

        val delta = (System.currentTimeMillis() - started).coerceAtLeast(0)
        _uiState.update { state ->
            val attempt = state.attempt ?: return@update state
            state.copy(attempt = attempt.copy(elapsedMs = attempt.elapsedMs + delta))
        }
        persist()
    }

    private fun liveElapsed(attempt: Attempt): Long {
        val started = runningSinceMs ?: return attempt.elapsedMs
        return attempt.elapsedMs + (System.currentTimeMillis() - started).coerceAtLeast(0)
    }

    // -- answering -----------------------------------------------------------

    fun selectOption(optionId: String) {
        val question = _uiState.value.currentQuestion ?: return
        updateAnswer(question) { answer ->
            val selection = when (question.type) {
                QuestionType.MCQ -> setOf(optionId)
                QuestionType.MSQ ->
                    if (optionId in answer.selectedOptionIds) {
                        answer.selectedOptionIds - optionId
                    } else {
                        answer.selectedOptionIds + optionId
                    }
                QuestionType.NAT -> answer.selectedOptionIds
            }
            answer.copy(selectedOptionIds = selection)
        }
    }

    fun setNumericInput(text: String) {
        val question = _uiState.value.currentQuestion ?: return
        if (question.type != QuestionType.NAT) return
        // Permit a leading minus, digits, and a single decimal point.
        if (text.isNotEmpty() && !NUMERIC_PATTERN.matches(text)) return
        updateAnswer(question) { it.copy(numericInput = text) }
    }

    fun clearResponse() {
        val question = _uiState.value.currentQuestion ?: return
        updateAnswer(question) {
            it.copy(selectedOptionIds = emptySet(), numericInput = "")
        }
    }

    fun toggleMarkForReview() {
        val question = _uiState.value.currentQuestion ?: return
        updateAnswer(question, markToggle = true) { it }
    }

    private fun updateAnswer(
        question: Question,
        markToggle: Boolean = false,
        transform: (AnswerState) -> AnswerState,
    ) {
        _uiState.update { state ->
            val attempt = state.attempt ?: return@update state
            val previous = attempt.answerFor(question.id)
            val updated = transform(previous)
            val marked = if (markToggle) !previous.status.isMarked else previous.status.isMarked

            state.copy(
                attempt = attempt.copy(
                    answers = attempt.answers + (question.id to updated.copy(
                        status = statusFor(updated, marked),
                    )),
                ),
            )
        }
        persist()
    }

    private fun statusFor(answer: AnswerState, marked: Boolean): QuestionStatus = when {
        answer.hasResponse && marked -> QuestionStatus.ANSWERED_AND_MARKED
        answer.hasResponse -> QuestionStatus.ANSWERED
        marked -> QuestionStatus.MARKED
        else -> QuestionStatus.NOT_ANSWERED
    }

    // -- navigation ----------------------------------------------------------

    fun next() = goTo(_uiState.value.currentIndex + 1)

    fun previous() = goTo(_uiState.value.currentIndex - 1)

    fun goTo(index: Int) {
        val state = _uiState.value
        val test = state.test ?: return
        val target = index.coerceIn(0, state.questions.lastIndex.coerceAtLeast(0))
        if (state.questions.isEmpty()) return

        bankQuestionTime()

        _uiState.update { current ->
            val attempt = current.attempt ?: return@update current
            current.copy(
                attempt = markVisited(attempt.copy(currentIndex = target), test, target),
                showPalette = false,
            )
        }
        questionEnteredAtMs = System.currentTimeMillis()
        persist()
    }

    /** A visited-but-untouched question is "not answered", not "not visited". */
    private fun markVisited(attempt: Attempt, test: MockTest, index: Int): Attempt {
        val question = test.orderedQuestions.getOrNull(index) ?: return attempt
        val existing = attempt.answers[question.id]
        if (existing != null) return attempt
        return attempt.copy(
            answers = attempt.answers + (question.id to AnswerState(
                questionId = question.id,
                status = QuestionStatus.NOT_ANSWERED,
            )),
        )
    }

    private fun bankQuestionTime() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        val spent = (System.currentTimeMillis() - questionEnteredAtMs).coerceAtLeast(0)
        if (spent == 0L) return
        _uiState.update { current ->
            val attempt = current.attempt ?: return@update current
            val answer = attempt.answerFor(question.id)
            current.copy(
                attempt = attempt.copy(
                    answers = attempt.answers + (question.id to answer.copy(
                        timeSpentMs = answer.timeSpentMs + spent,
                    )),
                ),
            )
        }
    }

    // -- palette & submission ------------------------------------------------

    fun showPalette(show: Boolean) = _uiState.update { it.copy(showPalette = show) }

    fun askToSubmit() = _uiState.update { it.copy(showSubmitConfirm = true) }

    fun dismissSubmit() = _uiState.update { it.copy(showSubmitConfirm = false) }

    fun submit(auto: Boolean = false) {
        val state = _uiState.value
        val test = state.test ?: return
        val current = state.attempt ?: return
        if (state.scorecard != null) return

        bankQuestionTime()
        tickerJob?.cancel()
        tickerJob = null

        val elapsed = liveElapsed(_uiState.value.attempt ?: current)
        runningSinceMs = null

        val finished = (_uiState.value.attempt ?: current).copy(
            elapsedMs = if (auto) current.totalDurationMs else elapsed,
            submittedAtEpochMs = System.currentTimeMillis(),
        )
        val scorecard = scoreAttempt(test, finished)

        _uiState.update {
            it.copy(
                attempt = finished,
                scorecard = scorecard,
                showSubmitConfirm = false,
                showPalette = false,
                remainingMs = 0,
            )
        }

        viewModelScope.launch {
            repository.clearAttempt(test.id)
            // Room rather than the old history file: a row per question is
            // what the subject and topic breakdowns are computed from.
            progress.record(scorecard, scorecard.timeTakenMs)
            // Recorded locally first, and uploaded whenever the network allows.
            // Doing it in that order is what makes finishing a paper on a train
            // work: the scorecard is never waiting on a request.
            requestSync()
        }
    }

    private fun persist() {
        val attempt = _uiState.value.attempt ?: return
        if (attempt.isSubmitted) return
        viewModelScope.launch {
            repository.saveAttempt(attempt.copy(elapsedMs = liveElapsed(attempt)))
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val TICK_MS = 500L
        val NUMERIC_PATTERN = Regex("^-?\\d*\\.?\\d*$")
    }
}
