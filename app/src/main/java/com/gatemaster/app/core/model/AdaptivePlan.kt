package com.gatemaster.app.core.model

import kotlin.math.min
import kotlin.math.pow

/**
 * What the app knows about one topic, from every question ever answered on it.
 *
 * This is the aggregate the Room analytics already compute; nothing new is
 * recorded for adaptive practice, which is the point. The data to decide what
 * to revise next has been sitting there since attempt history moved into a
 * database -- it was simply never read back.
 */
data class TopicHistory(
    val topicId: String,
    val subjectId: String,
    val attempted: Int,
    val correct: Int,
    val lastAttemptedEpochMs: Long,
)

/** Why a topic is in the next set, so the UI can say so rather than be a black box. */
enum class PracticeReason(val label: String) {
    /** Answered wrong often enough that it is the biggest hole. */
    WEAK("Often wrong"),

    /** Known well once, but long enough ago to be worth checking. */
    DUE("Due for review"),

    /** Never practised. Everything has to enter the rotation somehow. */
    UNSEEN("Not practised yet"),
}

data class TopicPriority(
    val topicId: String,
    val subjectId: String,
    val score: Double,
    val reason: PracticeReason,
    /** 0..1. Confidence-smoothed, so it is not simply correct / attempted. */
    val mastery: Double,
)

/**
 * Deciding what to practise next.
 *
 * The rest of the app answers "how did that paper go". This answers the
 * question a scorecard cannot: *what should I do tomorrow*. It is the only part
 * of GateMaster that makes a recommendation, so its reasoning is written out
 * rather than tuned until the output looked plausible.
 *
 * Three things decide a topic's priority, multiplied together:
 *
 * 1. **Weakness.** One minus mastery. A topic answered wrong most of the time
 *    is worth more revision than one answered right.
 *
 * 2. **Dueness.** How long it has been, measured against an interval that grows
 *    with mastery -- the spaced-repetition idea. A topic you know cold is not
 *    worth a question today just because you have not seen it this week; a
 *    topic you barely know is worth one tomorrow.
 *
 * 3. **Exam weight.** A subject worth 15 marks in the paper earns more practice
 *    than one worth 3. This is the part a generic flashcard scheduler cannot
 *    do, and it is the reason to write one for this app rather than import one.
 *
 * Everything here is a pure function of its inputs, so the whole scheduler is
 * tested on the JVM without a database, a clock, or a device.
 */
object AdaptivePlan {

    /**
     * Smoothing prior, in units of questions.
     *
     * Without it, one lucky answer reads as 100% mastery and the topic drops
     * out of rotation for three weeks, while one unlucky answer reads as a
     * catastrophic weakness. Starting every topic off as though it had already
     * been seen [PRIOR_TOTAL] times with [PRIOR_CORRECT] right means a single
     * answer moves the estimate a little and ten answers move it a lot, which
     * is the behaviour a student would expect.
     */
    const val PRIOR_CORRECT = 1.0
    const val PRIOR_TOTAL = 3.0

    /** A topic at zero mastery is worth revisiting tomorrow. */
    const val MIN_INTERVAL_DAYS = 1.0

    /** A topic at full mastery is worth revisiting in three weeks. */
    const val MAX_INTERVAL_DAYS = 21.0

    /**
     * How much being overdue can multiply a topic's priority.
     *
     * Capped, because a topic last seen a year ago is not a hundred times more
     * urgent than one last seen last week -- and without the cap a single
     * ancient topic would crowd out everything else for ever.
     */
    const val MAX_DUENESS = 3.0

    /** Below this, a topic is a weakness rather than a review. */
    const val WEAK_THRESHOLD = 0.6

    private const val DAY_MS = 24 * 60 * 60 * 1000.0

    /**
     * Confidence-smoothed accuracy, 0..1.
     *
     * A topic never attempted comes out at the prior rather than at zero: it is
     * unknown, not known to be bad, and treating unknown as terrible would
     * bury every genuine weakness under everything the user has yet to touch.
     */
    fun mastery(attempted: Int, correct: Int): Double =
        (correct + PRIOR_CORRECT) / (attempted + PRIOR_TOTAL)

    /** How long to leave a topic alone, given how well it is known. */
    fun intervalDays(mastery: Double): Double =
        MIN_INTERVAL_DAYS * (MAX_INTERVAL_DAYS / MIN_INTERVAL_DAYS).pow(mastery.coerceIn(0.0, 1.0))

    /**
     * Ranks [history] by what is worth practising now, most urgent first.
     *
     * [subjectWeights] maps a subject id to its marks in the paper; a subject
     * missing from it is treated as average rather than ignored, because the
     * 22 GATE papers that carry only an outline have no per-subject weightage
     * and their topics still deserve to be scheduled.
     */
    fun prioritise(
        history: List<TopicHistory>,
        subjectWeights: Map<String, Int> = emptyMap(),
        now: Long,
    ): List<TopicPriority> {
        val heaviest = subjectWeights.values.maxOrNull()?.takeIf { it > 0 }?.toDouble()

        return history.map { topic ->
            val mastery = mastery(topic.attempted, topic.correct)
            val weakness = 1.0 - mastery

            val dueness = if (topic.attempted == 0 || topic.lastAttemptedEpochMs <= 0) {
                // Never practised: maximally due by definition. It still ranks
                // below a genuinely weak topic, because its weakness term is
                // only the prior rather than a measured failure.
                MAX_DUENESS
            } else {
                val daysSince = (now - topic.lastAttemptedEpochMs) / DAY_MS
                min(daysSince / intervalDays(mastery), MAX_DUENESS)
            }

            // Half the weight is the exam's, half is flat -- so a light subject
            // is practised less, never not at all.
            val weight = if (heaviest == null) {
                1.0
            } else {
                0.5 + 0.5 * ((subjectWeights[topic.subjectId] ?: 0) / heaviest).coerceIn(0.0, 1.0)
            }

            TopicPriority(
                topicId = topic.topicId,
                subjectId = topic.subjectId,
                // The dueness floor keeps a topic seen an hour ago from
                // scoring exactly zero, which would drop it out of the ranking
                // rather than merely down it.
                score = weakness * maxOf(dueness, 0.05) * weight,
                reason = when {
                    topic.attempted == 0 -> PracticeReason.UNSEEN
                    mastery < WEAK_THRESHOLD -> PracticeReason.WEAK
                    else -> PracticeReason.DUE
                },
                mastery = mastery,
            )
        }.sortedByDescending { it.score }
    }

    /**
     * Splits [questionCount] questions across the highest-priority topics.
     *
     * Not simply "all of them from the worst topic": twenty questions on the
     * single weakest topic is a worse revision session than a spread across the
     * five that need work, and it makes the set repetitive enough that people
     * stop sitting them.
     *
     * The share is proportional to priority, so the worst topic still gets the
     * most. Every chosen topic gets at least one question -- a topic allocated
     * zero should not have been chosen.
     */
    fun allocate(
        priorities: List<TopicPriority>,
        questionCount: Int,
        maxTopics: Int = DEFAULT_MAX_TOPICS,
    ): Map<String, Int> {
        if (questionCount <= 0 || priorities.isEmpty()) return emptyMap()

        // Never more topics than questions, or the allocation cannot give each
        // of them one.
        val chosen = priorities.take(minOf(maxTopics, questionCount))
        val total = chosen.sumOf { it.score }

        if (total <= 0.0) {
            // Everything scored zero, which happens when every topic was
            // practised moments ago. Spread evenly rather than returning
            // nothing: the user asked for a set.
            return chosen.evenly(questionCount)
        }

        val allocation = LinkedHashMap<String, Int>()
        var assigned = 0
        for (topic in chosen) {
            val share = ((topic.score / total) * questionCount).toInt().coerceAtLeast(1)
            allocation[topic.topicId] = share
            assigned += share
        }

        // Rounding down leaves a remainder; rounding each up can overshoot.
        // Settle the difference on the highest-priority topics, one at a time.
        var difference = questionCount - assigned
        var index = 0
        while (difference != 0 && chosen.isNotEmpty()) {
            val topic = chosen[index % chosen.size].topicId
            val current = allocation.getValue(topic)
            if (difference > 0) {
                allocation[topic] = current + 1
                difference--
            } else if (current > 1) {
                allocation[topic] = current - 1
                difference++
            }
            index++
            // Every topic is already down to its floor of one and the total is
            // still too high, which cannot happen given `chosen` is capped at
            // questionCount -- but a loop that cannot end is worse than a set
            // one question short.
            if (index > chosen.size * questionCount + chosen.size) break
        }

        return allocation
    }

    private fun List<TopicPriority>.evenly(questionCount: Int): Map<String, Int> {
        val each = questionCount / size
        val remainder = questionCount % size
        return mapIndexed { index, topic ->
            topic.topicId to each + if (index < remainder) 1 else 0
        }.filter { it.second > 0 }.toMap()
    }

    /**
     * How many topics one set spans.
     *
     * Six is a revision session; twenty is a survey that teaches nothing.
     */
    const val DEFAULT_MAX_TOPICS = 6

    /** How many questions an adaptive set holds. */
    const val QUESTION_COUNT = 15
}
