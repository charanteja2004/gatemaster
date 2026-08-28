package com.gatemaster.app.core.data

import android.util.Log
import com.gatemaster.app.core.data.db.AttemptDao
import com.gatemaster.app.core.data.db.AttemptEntity
import com.gatemaster.app.core.data.db.ProgressTotals
import com.gatemaster.app.core.data.db.QuestionResultEntity
import com.gatemaster.app.core.data.db.ScorePoint
import com.gatemaster.app.core.data.db.SubjectAccuracy
import com.gatemaster.app.core.data.db.TopicAccuracy
import com.gatemaster.app.core.model.Scorecard
import com.gatemaster.app.core.model.TopicHistory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Attempt history, and the analytics built on it.
 *
 * A finished paper tells you today's score. The point of keeping every
 * question of every attempt is the question a single scorecard cannot answer:
 * which subjects and topics are actually costing marks, and whether that is
 * improving.
 */
class ProgressRepository(
    private val dao: AttemptDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis,
    /** Injectable so a test can assert on a stable id instead of a random one. */
    private val newClientId: () -> String = { java.util.UUID.randomUUID().toString() },
) {

    fun totals(): Flow<ProgressTotals> = dao.totals()

    fun subjectAccuracy(): Flow<List<SubjectAccuracy>> = dao.subjectAccuracy()

    fun weakestTopics(): Flow<List<TopicAccuracy>> = dao.weakestTopics()

    fun scoreTrend(): Flow<List<ScorePoint>> = dao.scoreTrend()

    fun recentAttempts(): Flow<List<AttemptEntity>> = dao.recentAttempts()

    /** Per-topic history, which is what adaptive practice is drawn from. */
    suspend fun topicHistory(): List<TopicHistory> = withContext(io) {
        runCatching { dao.topicHistory() }.getOrDefault(emptyList())
    }

    /** Stores a finished sitting: one attempt row and one row per question. */
    suspend fun record(scorecard: Scorecard, timeTakenMs: Long) = withContext(io) {
        runCatching {
            dao.record(
                attempt = AttemptEntity(
                    testId = scorecard.test.id,
                    title = scorecard.test.title,
                    submittedAtEpochMs = scorecard.attempt.submittedAtEpochMs ?: now(),
                    score = scorecard.score,
                    maxMarks = scorecard.maxMarks,
                    correct = scorecard.correct,
                    incorrect = scorecard.incorrect,
                    unattempted = scorecard.unattempted,
                    timeTakenMs = timeTakenMs,
                    // Assigned here, at the moment the attempt becomes a fact,
                    // and never changed. Sync uses it as the idempotency key,
                    // so it has to exist whether or not anyone is signed in --
                    // an attempt sat offline still has to upload cleanly when
                    // an account is added later.
                    clientAttemptId = newClientId(),
                ),
                results = scorecard.results.map { result ->
                    QuestionResultEntity(
                        attemptId = 0,
                        questionId = result.question.id,
                        subjectId = result.question.subjectId,
                        // Generated questions carry their topic id here.
                        topicId = result.question.topic,
                        marks = result.question.marks,
                        marksAwarded = result.marksAwarded,
                        kind = result.kind.name,
                    )
                },
            )
        }.onFailure { Log.e(TAG, "Could not record attempt", it) }
        Unit
    }

    /**
     * Brings forward the attempts recorded before there was a database.
     *
     * Only the totals survive, because the old file never stored per-question
     * results — so those attempts count towards the trend but contribute
     * nothing to the subject and topic breakdowns. Losing that history
     * entirely would be worse than carrying it in partially.
     */
    suspend fun importLegacyHistory(history: List<AttemptRecord>) = withContext(io) {
        if (history.isEmpty() || dao.count() > 0) return@withContext
        runCatching {
            history.sortedBy { it.submittedAtEpochMs }.forEach { record ->
                dao.record(
                    attempt = AttemptEntity(
                        testId = record.testId,
                        title = record.testTitle,
                        submittedAtEpochMs = record.submittedAtEpochMs,
                        score = record.score,
                        maxMarks = record.maxMarks,
                        correct = record.correct,
                        incorrect = record.incorrect,
                        unattempted = record.unattempted,
                        timeTakenMs = record.timeTakenMs,
                        clientAttemptId = newClientId(),
                    ),
                    results = emptyList(),
                )
            }
        }.onFailure { Log.e(TAG, "Could not import the old attempt history", it) }
        Unit
    }

    private companion object {
        const val TAG = "ProgressRepository"
    }
}
