package com.gatemaster.app.core.data.db

import androidx.room.Dao
import com.gatemaster.app.core.model.TopicHistory
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    // --- Sync ---------------------------------------------------------------

    /**
     * Attempts this device has not yet handed to the server, oldest first.
     *
     * Oldest first so a client that has been offline for a month uploads its
     * history in the order it happened, and a truncated batch still leaves the
     * server with a prefix rather than a scatter.
     */
    @Query("SELECT * FROM attempts WHERE syncedAt IS NULL ORDER BY id LIMIT :limit")
    suspend fun unsyncedAttempts(limit: Int): List<AttemptEntity>

    @Query("SELECT * FROM question_results WHERE attemptId IN (:attemptIds)")
    suspend fun resultsFor(attemptIds: List<Long>): List<QuestionResultEntity>

    @Query("UPDATE attempts SET syncedAt = :at WHERE clientAttemptId IN (:clientIds)")
    suspend fun markSynced(clientIds: List<String>, at: Long)

    /** Where the next download starts. Zero when nothing has ever come down. */
    @Query("SELECT COALESCE(MAX(serverSeq), 0) FROM attempts")
    suspend fun highestServerSeq(): Long

    @Query("SELECT clientAttemptId FROM attempts WHERE clientAttemptId IN (:clientIds)")
    suspend fun existingClientIds(clientIds: List<String>): List<String>

    /**
     * Records where the server filed an attempt this device already had.
     *
     * Without this the download cursor never moves past the rows this device
     * uploaded -- they come back down, are recognised as known, are skipped,
     * and MAX(serverSeq) stays where it was. The same page would then be
     * fetched on every sync for ever.
     */
    @Query(
        """
        UPDATE attempts
        SET serverSeq = :serverSeq, syncedAt = COALESCE(syncedAt, :at)
        WHERE clientAttemptId = :clientId
        """,
    )
    suspend fun recordServerSeq(clientId: String, serverSeq: Long, at: Long)

    /**
     * Stores an attempt that came down from the server.
     *
     * IGNORE rather than REPLACE on the conflict: an attempt is immutable, so
     * a second copy carries nothing new, and REPLACE would delete the existing
     * row -- taking its questions with it through the foreign key cascade, then
     * re-inserting them. IGNORE simply does nothing, which is correct and also
     * what makes this safe to call on every sync.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDownloaded(attempt: AttemptEntity): Long

    @Transaction
    suspend fun storeDownloaded(attempt: AttemptEntity, results: List<QuestionResultEntity>) {
        val id = insertDownloaded(attempt)
        // -1 means the unique index rejected it: this device already had it.
        if (id == -1L) return
        insertResults(results.map { it.copy(attemptId = id) })
    }

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

    /**
     * Per topic: how many questions were answered, how many were right, and
     * when it was last seen.
     *
     * This is what adaptive practice runs on, and it is deliberately the same
     * grain the Progress tab already aggregates -- the join to `attempts` is
     * only there for the timestamp, which lives on the sitting rather than on
     * the question.
     */
    @Query(
        """
        SELECT r.topicId AS topicId,
               r.subjectId AS subjectId,
               COUNT(*) AS attempted,
               SUM(CASE WHEN r.kind = 'CORRECT' THEN 1 ELSE 0 END) AS correct,
               MAX(a.submittedAtEpochMs) AS lastAttemptedEpochMs
        FROM question_results r
        JOIN attempts a ON a.id = r.attemptId
        WHERE r.topicId IS NOT NULL
          AND r.subjectId IS NOT NULL
          AND r.kind != 'UNATTEMPTED'
        GROUP BY r.topicId, r.subjectId
        """,
    )
    suspend fun topicHistory(): List<TopicHistory>

    @Query("SELECT COUNT(*) FROM attempts")
    suspend fun count(): Int

    @Query("DELETE FROM attempts")
    suspend fun clear()
}
