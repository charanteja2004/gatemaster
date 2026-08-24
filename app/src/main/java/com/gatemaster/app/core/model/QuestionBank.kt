package com.gatemaster.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Questions tagged by topic, so a test can be assembled on demand.
 *
 * A three-hour mock is the wrong shape for a phone: almost nobody sits one on
 * a bus. Tagging every question with the topic it belongs to lets the app build
 * a ten-question set for one topic, or a longer set for a whole subject, from
 * the same bank.
 */

@Serializable
enum class Difficulty {
    @SerialName("easy")
    EASY,

    @SerialName("medium")
    MEDIUM,

    @SerialName("hard")
    HARD,
}

/** A question in the bank, before it is placed into a generated test. */
@Serializable
data class BankQuestion(
    val id: String,
    /** Matches a topic id in content_index.json. Null means subject-wide. */
    val topicId: String? = null,
    val type: QuestionType = QuestionType.MCQ,
    val marks: Int = 1,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val text: String,
    val options: List<Option> = emptyList(),
    val correctOptionIds: List<String> = emptyList(),
    val numericAnswer: NumericAnswer? = null,
    val solution: String? = null,
) {
    fun toQuestion(number: Int, subjectId: String, topicTitle: String?): Question = Question(
        id = id,
        number = number,
        type = type,
        marks = marks,
        text = text,
        options = options,
        correctOptionIds = correctOptionIds,
        numericAnswer = numericAnswer,
        subjectId = subjectId,
        topic = topicTitle,
        solution = solution,
    )
}

@Serializable
data class SubjectQuestionBank(
    val subjectId: String,
    val questions: List<BankQuestion> = emptyList(),
) {
    fun forTopic(topicId: String): List<BankQuestion> =
        questions.filter { it.topicId == topicId }

    val topicIds: Set<String> get() = questions.mapNotNull { it.topicId }.toSet()
}

@Serializable
data class QuestionBankIndex(
    val schemaVersion: Int = 1,
    /** subjectId -> asset path of that subject's bank. */
    val banks: Map<String, String> = emptyMap(),
)

/**
 * A test the app assembles rather than ships.
 *
 * The id encodes what to build so the player can be handed a plain id and stay
 * unaware that the test did not come from a file.
 */
data class QuickTestSpec(
    val subjectId: String,
    val topicId: String? = null,
) {
    val id: String get() = if (topicId != null) "$PREFIX$subjectId:$topicId" else "$PREFIX$subjectId"

    companion object {
        const val PREFIX = "quick:"

        /** Roughly two minutes a question, which is the GATE pace. */
        fun durationFor(questionCount: Int): Int = (questionCount * 2).coerceIn(5, 60)

        fun parse(testId: String): QuickTestSpec? {
            if (!testId.startsWith(PREFIX)) return null
            val body = testId.removePrefix(PREFIX)
            val parts = body.split(":", limit = 2)
            return QuickTestSpec(
                subjectId = parts[0],
                topicId = parts.getOrNull(1)?.takeIf { it.isNotBlank() },
            )
        }
    }
}
