package com.gatemaster.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Everything the progress screen asks of the attempt history.
 *
 * The aggregates are computed in SQL rather than in Kotlin so that the app
 * never loads every question of every attempt into memory to answer "how am I
 * doing" — which is the whole argument for a database here.
 */
@Dao
interface AttemptDao {

    @Insert
    suspend fun insertAttempt(attempt: AttemptEntity): Long

    @Insert
    suspend fun insertResults(results: List<QuestionResultEntity>)

    /** An attempt and its questions land together or not at all. */
    @Transaction
    suspend fun record(attempt: AttemptEntity, results: List<QuestionResultEntity>) {
        val id = insertAttempt(attempt)
        insertResults(results.map { it.copy(attemptId = id) })
    }

    @Query("SELECT * FROM attempts ORDER BY submittedAtEpochMs DESC LIMIT :limit")
    fun recentAttempts(limit: Int = 20): Flow<List<AttemptEntity>>

    @Query(
        """
        SELECT COUNT(*) AS attempts,
               COALESCE(SUM(correct), 0) AS correct,
               COALESCE(SUM(incorrect), 0) AS incorrect,
               COALESCE(SUM(unattempted), 0) AS unattempted,
               COALESCE(SUM(timeTakenMs), 0) AS timeSpentMs
        FROM attempts
        """,
    )
    fun totals(): Flow<ProgressTotals>

    /**
     * Unattempted questions are excluded: accuracy is about the ones actually
     * answered, and counting blanks as wrong would punish running out of time
     * rather than not knowing the subject.
     */
    @Query(
        """
        SELECT subjectId AS subjectId,
               COUNT(*) AS attempted,
               SUM(CASE WHEN kind = 'CORRECT' THEN 1 ELSE 0 END) AS correct,
               SUM(marksAwarded) AS marksAwarded,
               SUM(marks) AS marksAvailable
        FROM question_results
        WHERE subjectId IS NOT NULL AND kind != 'UNATTEMPTED'
        GROUP BY subjectId
        ORDER BY (CAST(SUM(CASE WHEN kind = 'CORRECT' THEN 1 ELSE 0 END) AS REAL) / COUNT(*)) ASC
        """,
    )
    fun subjectAccuracy(): Flow<List<SubjectAccuracy>>

    /**
     * Worst first. [minAttempted] keeps a single unlucky question from being
     * reported as a weakness.
     */
    @Query(
        """
        SELECT topicId AS topicId,
               subjectId AS subjectId,
               COUNT(*) AS attempted,
               SUM(CASE WHEN kind = 'CORRECT' THEN 1 ELSE 0 END) AS correct
        FROM question_results
        WHERE topicId IS NOT NULL AND kind != 'UNATTEMPTED'
        GROUP BY topicId
        HAVING COUNT(*) >= :minAttempted
        ORDER BY (CAST(SUM(CASE WHEN kind = 'CORRECT' THEN 1 ELSE 0 END) AS REAL) / COUNT(*)) ASC,
                 COUNT(*) DESC
        LIMIT :limit
        """,
    )
    fun weakestTopics(minAttempted: Int = 3, limit: Int = 8): Flow<List<TopicAccuracy>>

    /** Oldest first, because a trend is read left to right. */
    @Query(
        """
        SELECT submittedAtEpochMs, score, maxMarks
        FROM attempts
        ORDER BY submittedAtEpochMs ASC
        LIMIT :limit
        """,
    )
    fun scoreTrend(limit: Int = 30): Flow<List<ScorePoint>>

    @Query("SELECT COUNT(*) FROM attempts")
    suspend fun count(): Int

    @Query("DELETE FROM attempts")
    suspend fun clear()
}
