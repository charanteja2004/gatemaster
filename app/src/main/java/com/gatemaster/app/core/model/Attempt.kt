package com.gatemaster.app.core.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * The state of one sitting of a test, and the rules for scoring it.
 *
 * Everything here is a pure function of the attempt plus the paper, so the
 * scoring rules are unit-testable without a device. The previous engine kept
 * its state in Activity fields and only advanced the question index when the
 * answer was correct, which trapped the user on any question they got wrong.
 */

/** Mirrors the status colours of the real GATE question palette. */
@Serializable
enum class QuestionStatus {
    NOT_VISITED,
    NOT_ANSWERED,
    ANSWERED,
    MARKED,
    ANSWERED_AND_MARKED,
    ;

    val isAnswered: Boolean get() = this == ANSWERED || this == ANSWERED_AND_MARKED
    val isMarked: Boolean get() = this == MARKED || this == ANSWERED_AND_MARKED
}

@Serializable
data class AnswerState(
    val questionId: String,
    val selectedOptionIds: Set<String> = emptySet(),
    /** Raw text as typed, so "3." and "-0" round-trip while the user is typing. */
    val numericInput: String = "",
    val status: QuestionStatus = QuestionStatus.NOT_VISITED,
    val timeSpentMs: Long = 0,
) {
    val hasResponse: Boolean
        get() = selectedOptionIds.isNotEmpty() || numericInput.isNotBlank()

    val numericValue: Double? get() = numericInput.trim().toDoubleOrNull()
}

@Serializable
data class Attempt(
    val testId: String,
    val startedAtEpochMs: Long,
    val durationMinutes: Int,
    /** Elapsed time banked across pauses. */
    val elapsedMs: Long = 0,
    val currentIndex: Int = 0,
    val answers: Map<String, AnswerState> = emptyMap(),
    val submittedAtEpochMs: Long? = null,
) {
    val isSubmitted: Boolean get() = submittedAtEpochMs != null

    val totalDurationMs: Long get() = durationMinutes * 60_000L

    val remainingMs: Long get() = (totalDurationMs - elapsedMs).coerceAtLeast(0)

    val isTimeUp: Boolean get() = remainingMs == 0L

    fun answerFor(questionId: String): AnswerState =
        answers[questionId] ?: AnswerState(questionId)

    fun statusCounts(): Map<QuestionStatus, Int> =
        answers.values.groupingBy { it.status }.eachCount()
}

// ---------------------------------------------------------------------------
// Scoring
// ---------------------------------------------------------------------------

enum class ResultKind { CORRECT, INCORRECT, UNATTEMPTED }

data class QuestionResult(
    val question: Question,
    val answer: AnswerState,
    val kind: ResultKind,
    val marksAwarded: Double,
) {
    val isCorrect: Boolean get() = kind == ResultKind.CORRECT
}

data class SectionScore(
    val name: String,
    val score: Double,
    val maxMarks: Int,
    val correct: Int,
    val incorrect: Int,
    val unattempted: Int,
)

data class Scorecard(
    val test: MockTest,
    val attempt: Attempt,
    val results: List<QuestionResult>,
    val sections: List<SectionScore>,
) {
    val score: Double get() = results.sumOf { it.marksAwarded }
    val maxMarks: Int get() = test.totalMarks
    val correct: Int get() = results.count { it.kind == ResultKind.CORRECT }
    val incorrect: Int get() = results.count { it.kind == ResultKind.INCORRECT }
    val unattempted: Int get() = results.count { it.kind == ResultKind.UNATTEMPTED }
    val attempted: Int get() = correct + incorrect

    /** Share of attempted questions that were right. Zero if nothing attempted. */
    val accuracy: Float
        get() = if (attempted == 0) 0f else correct.toFloat() / attempted

    val percentage: Float
        get() = if (maxMarks == 0) 0f else (score / maxMarks).toFloat() * 100f

    val timeTakenMs: Long get() = attempt.elapsedMs

    /** Marks lost to negative marking — the number people most want to see. */
    val marksLost: Double
        get() = results.filter { it.kind == ResultKind.INCORRECT }.sumOf { -it.marksAwarded }

    val scoreDisplay: String get() = formatMarks(score)
}

/** GATE reports marks to two decimals; whole numbers print without them. */
fun formatMarks(value: Double): String {
    val rounded = (value * 100).roundToInt() / 100.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        String.format("%.2f", rounded)
    }
}

/**
 * Grades one question.
 *
 * MSQ is all-or-nothing: GATE awards no partial credit, and deducts nothing
 * for a wrong selection.
 */
fun gradeQuestion(question: Question, answer: AnswerState): QuestionResult {
    if (!answer.hasResponse) {
        return QuestionResult(question, answer, ResultKind.UNATTEMPTED, 0.0)
    }

    val correct = when (question.type) {
        QuestionType.MCQ,
        QuestionType.MSQ,
        -> answer.selectedOptionIds == question.correctOptionIds.toSet()

        QuestionType.NAT -> {
            val value = answer.numericValue
            value != null && question.numericAnswer?.accepts(value) == true
        }
    }

    return if (correct) {
        QuestionResult(question, answer, ResultKind.CORRECT, question.marks.toDouble())
    } else {
        QuestionResult(question, answer, ResultKind.INCORRECT, -question.negativeMarks)
    }
}

fun scoreAttempt(test: MockTest, attempt: Attempt): Scorecard {
    val results = test.orderedQuestions.map { question ->
        gradeQuestion(question, attempt.answerFor(question.id))
    }
    val byId = results.associateBy { it.question.id }

    val sections = test.sections.map { section ->
        val sectionResults = section.questionIds.mapNotNull { byId[it] }
        SectionScore(
            name = section.name,
            score = sectionResults.sumOf { it.marksAwarded },
            maxMarks = sectionResults.sumOf { it.question.marks },
            correct = sectionResults.count { it.kind == ResultKind.CORRECT },
            incorrect = sectionResults.count { it.kind == ResultKind.INCORRECT },
            unattempted = sectionResults.count { it.kind == ResultKind.UNATTEMPTED },
        )
    }

    return Scorecard(test, attempt, results, sections)
}
