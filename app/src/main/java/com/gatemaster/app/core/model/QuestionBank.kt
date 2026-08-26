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
    /**
     * Display name, so a mixed paper can label its sections without loading the
     * whole content index. Falls back to the id when a bank predates the field.
     */
    val subjectName: String = "",
    val questions: List<BankQuestion> = emptyList(),
) {
    fun forTopic(topicId: String): List<BankQuestion> =
        questions.filter { it.topicId == topicId }

    val topicIds: Set<String> get() = questions.mapNotNull { it.topicId }.toSet()

    val displayName: String get() = subjectName.ifBlank { subjectId }

    /** Questions grouped by topic. Untagged questions form a group of their own. */
    fun byTopic(): List<List<BankQuestion>> =
        questions.groupBy { it.topicId }.values.toList()

    fun countByTopic(): Map<String, Int> =
        questions.mapNotNull { it.topicId }.groupingBy { it }.eachCount()
}

@Serializable
data class QuestionBankIndex(
    val schemaVersion: Int = 1,
    /** subjectId -> asset path of that subject's bank. */
    val banks: Map<String, String> = emptyMap(),
)

/**
 * The three sittings a practice set can be.
 *
 * They exist because one shape does not fit how people actually revise: a
 * single topic is what fits a spare five minutes, a subject is a study block,
 * and a mixed paper is how you find out which subject is weakest — which a
 * subject-at-a-time test can never tell you.
 */
enum class PracticeMode(val label: String, val questionLimit: Int) {
    TOPIC("Topic practice", 10),
    SUBJECT("Subject practice", 20),
    MIXED("Mixed test", 30),
}

/**
 * A test the app assembles rather than ships.
 *
 * The id encodes what to build, so the player can be handed a plain test id and
 * stay unaware that the test did not come from a file.
 */
data class PracticeSpec(
    val mode: PracticeMode,
    /** Empty in [PracticeMode.MIXED] means every subject that has a bank. */
    val subjectIds: List<String> = emptyList(),
    val topicId: String? = null,
) {
    val id: String
        get() = when (mode) {
            PracticeMode.TOPIC -> "${PREFIX}topic:${subjectIds.first()}:$topicId"
            PracticeMode.SUBJECT -> "${PREFIX}subject:${subjectIds.first()}"
            PracticeMode.MIXED ->
                PREFIX + "mixed:" +
                    if (subjectIds.isEmpty()) EVERY_SUBJECT else subjectIds.joinToString("+")
        }

    companion object {
        const val PREFIX = "practice:"

        /** The id segment standing for "whatever subjects have questions". */
        const val EVERY_SUBJECT = "all"

        /** Ids written before practice grew past one subject at a time. */
        private const val LEGACY_PREFIX = "quick:"

        fun topic(subjectId: String, topicId: String) =
            PracticeSpec(PracticeMode.TOPIC, listOf(subjectId), topicId)

        fun subject(subjectId: String) =
            PracticeSpec(PracticeMode.SUBJECT, listOf(subjectId))

        /** Pass no subjects for a mix across everything that has questions. */
        fun mixed(subjectIds: List<String> = emptyList()) =
            PracticeSpec(PracticeMode.MIXED, subjectIds.distinct())

        /** Roughly two minutes a question, which is the GATE pace. */
        fun durationFor(questionCount: Int): Int = (questionCount * 2).coerceIn(5, 60)

        fun parse(testId: String): PracticeSpec? = when {
            testId.startsWith(PREFIX) -> parseCurrent(testId.removePrefix(PREFIX))
            testId.startsWith(LEGACY_PREFIX) -> parseLegacy(testId.removePrefix(LEGACY_PREFIX))
            else -> null
        }

        private fun parseCurrent(body: String): PracticeSpec? {
            val (kind, rest) = body.split(":", limit = 2).let {
                it[0] to it.getOrElse(1) { "" }
            }
            if (rest.isBlank()) return null
            return when (kind) {
                "topic" -> rest.split(":", limit = 2)
                    .takeIf { it.size == 2 && it.all(String::isNotBlank) }
                    ?.let { topic(it[0], it[1]) }

                "subject" -> subject(rest)

                "mixed" -> if (rest == EVERY_SUBJECT) {
                    mixed()
                } else {
                    mixed(rest.split("+").filter(String::isNotBlank))
                }

                else -> null
            }
        }

        /**
         * A saved attempt outlives the id scheme that created it. Reading the
         * old form costs four lines and means an in-progress test from a
         * previous version still resumes.
         */
        private fun parseLegacy(body: String): PracticeSpec? {
            val parts = body.split(":", limit = 2)
            val subjectId = parts[0].takeIf { it.isNotBlank() } ?: return null
            val topicId = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
            return if (topicId != null) topic(subjectId, topicId) else subject(subjectId)
        }
    }
}
