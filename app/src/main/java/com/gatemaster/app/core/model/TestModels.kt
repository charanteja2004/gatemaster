package com.gatemaster.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Test and question model, shaped to the real GATE paper rather than to a
 * flashcard loop.
 *
 * GATE uses three question types, and they do not score alike: only
 * single-answer MCQs carry negative marking. The old engine modelled a single
 * type, had no marks, no timer and no scoring at all.
 */

@Serializable
enum class QuestionType {
    /** Single correct option. The only type with negative marking. */
    @SerialName("mcq")
    MCQ,

    /** One or more correct options. No negative marking, no partial credit. */
    @SerialName("msq")
    MSQ,

    /** Numerical Answer Type — the candidate types a number. No negative marking. */
    @SerialName("nat")
    NAT,
}

@Serializable
data class Option(
    val id: String,
    val text: String,
)

/**
 * GATE accepts a numeric answer within a tolerance range, published as
 * "3.14 to 3.16" in the official keys.
 */
@Serializable
data class NumericAnswer(
    val min: Double,
    val max: Double,
) {
    fun accepts(value: Double): Boolean = value in min..max

    val display: String
        get() = if (min == max) trim(min) else "${trim(min)} to ${trim(max)}"

    private fun trim(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}

@Serializable
data class Question(
    val id: String,
    val number: Int,
    val type: QuestionType,
    val marks: Int,
    val text: String,
    /** Empty for [QuestionType.NAT]. */
    val options: List<Option> = emptyList(),
    /** Option ids. Exactly one for MCQ, one or more for MSQ, empty for NAT. */
    val correctOptionIds: List<String> = emptyList(),
    /** Set only for [QuestionType.NAT]. */
    val numericAnswer: NumericAnswer? = null,
    /** Subject this question belongs to, for per-topic analytics. */
    val subjectId: String? = null,
    val topic: String? = null,
    val solution: String? = null,
) {
    /**
     * Marks deducted for a wrong answer. GATE deducts 1/3 of the question's
     * marks on MCQs only; MSQ and NAT carry no penalty.
     */
    val negativeMarks: Double
        get() = if (type == QuestionType.MCQ) marks / 3.0 else 0.0

    val hasSolution: Boolean get() = !solution.isNullOrBlank()
}

@Serializable
data class TestSection(
    val id: String,
    val name: String,
    val questionIds: List<String>,
)

@Serializable
data class MockTest(
    val id: String,
    val title: String,
    val description: String = "",
    val durationMinutes: Int,
    val sections: List<TestSection>,
    val questions: List<Question>,
) {
    val questionCount: Int get() = questions.size

    val totalMarks: Int get() = questions.sumOf { it.marks }

    private val byId: Map<String, Question> by lazy { questions.associateBy { it.id } }

    fun question(id: String): Question? = byId[id]

    /** Questions in paper order: section by section, in the listed order. */
    val orderedQuestions: List<Question> by lazy {
        sections.flatMap { section -> section.questionIds.mapNotNull { byId[it] } }
    }

    fun sectionOf(questionId: String): TestSection? =
        sections.firstOrNull { questionId in it.questionIds }
}

@Serializable
data class TestCatalogue(
    val schemaVersion: Int,
    val tests: List<TestSummary> = emptyList(),
)

/** Listing entry; the full [MockTest] is loaded only when an attempt starts. */
@Serializable
data class TestSummary(
    val id: String,
    val title: String,
    val description: String = "",
    val durationMinutes: Int,
    val questionCount: Int,
    val totalMarks: Int,
    val file: String,
)
