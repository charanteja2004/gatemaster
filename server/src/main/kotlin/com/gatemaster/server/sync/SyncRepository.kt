package com.gatemaster.server.sync

import com.gatemaster.protocol.AttemptPage
import com.gatemaster.protocol.ProgressResponse
import com.gatemaster.protocol.SyncedAttempt
import com.gatemaster.protocol.SyncedAttemptQuestion
import com.gatemaster.protocol.UploadResult
import com.gatemaster.server.auth.isUniqueViolation
import com.gatemaster.server.db.Database
import com.gatemaster.server.db.firstOrNull
import com.gatemaster.server.db.map
import com.gatemaster.server.db.query
import com.gatemaster.server.db.queryReturning
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class SyncRepository(private val database: Database) {

    // --- Study progress -----------------------------------------------------

    fun progressFor(userId: UUID): ProgressResponse? = database.read { connection ->
        connection.query("SELECT document, revision FROM study_progress WHERE user_id = ?") {
            setObject(1, userId)
            firstOrNull { ProgressResponse(it.getString("document"), it.getLong("revision")) }
        }
    }

    /**
     * Writes [document] if the caller was working from [expectedRevision],
     * and throws [ProgressConflict] carrying the current state if it was not.
     *
     * The read and the write are one transaction, and the UPDATE names the
     * expected revision in its WHERE clause. Checking first and writing after
     * would leave a window in which the other device's write lands between the
     * two, which is the exact interleaving this is here to reject.
     *
     * A first write uses expectedRevision 0, which is the revision of a row
     * that does not exist yet.
     */
    fun writeProgress(
        userId: UUID,
        document: String,
        expectedRevision: Long,
        now: Instant,
    ): ProgressResponse = database.transaction { connection ->
        val current = connection.query(
            "SELECT document, revision FROM study_progress WHERE user_id = ?",
        ) {
            setObject(1, userId)
            firstOrNull { ProgressResponse(it.getString("document"), it.getLong("revision")) }
        }

        if (current == null) {
            if (expectedRevision != 0L) {
                throw ProgressConflict(ProgressResponse(document = "", revision = 0))
            }
            val next = ProgressResponse(document, revision = 1)
            connection.query(
                """
                INSERT INTO study_progress (user_id, document, revision, updated_at)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
            ) {
                setObject(1, userId)
                setString(2, document)
                setLong(3, next.revision)
                setTimestamp(4, Timestamp.from(now))
                executeUpdate()
            }
            return@transaction next
        }

        if (current.revision != expectedRevision) throw ProgressConflict(current)

        val next = ProgressResponse(document, revision = current.revision + 1)
        val updated = connection.query(
            """
            UPDATE study_progress SET document = ?, revision = ?, updated_at = ?
            WHERE user_id = ? AND revision = ?
            """.trimIndent(),
        ) {
            setString(1, document)
            setLong(2, next.revision)
            setTimestamp(3, Timestamp.from(now))
            setObject(4, userId)
            setLong(5, expectedRevision)
            executeUpdate()
        }
        // Zero rows means another transaction committed between the SELECT and
        // the UPDATE. The row-level lock the UPDATE takes makes this the last
        // place a conflict can hide.
        if (updated == 0) throw ProgressConflict(progressFor(userId) ?: current)
        next
    }

    // --- Attempts -----------------------------------------------------------

    /**
     * Inserts each attempt the user does not already have.
     *
     * Duplicates are detected by the unique constraint rather than by looking
     * first, and each attempt gets its own savepoint-free transaction so one
     * duplicate in a batch of twenty does not roll back the other nineteen.
     */
    fun uploadAttempts(userId: UUID, attempts: List<SyncedAttempt>, now: Instant): UploadResult {
        val duplicates = mutableListOf<String>()
        var accepted = 0

        for (attempt in attempts) {
            try {
                database.transaction { connection ->
                    // The generated sequence is what the questions hang off, so
                    // it has to come back from the insert itself.
                    val seq = connection.queryReturning(
                        """
                        INSERT INTO attempts (
                            user_id, client_attempt_id, test_id, test_title,
                            started_at, finished_at, duration_seconds,
                            score, max_score, uploaded_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        generatedColumn = "server_seq",
                    ) {
                        setObject(1, userId)
                        setString(2, attempt.clientAttemptId)
                        setString(3, attempt.testId)
                        setString(4, attempt.testTitle)
                        setTimestamp(5, Timestamp.from(Instant.ofEpochMilli(attempt.startedAt)))
                        setTimestamp(6, Timestamp.from(Instant.ofEpochMilli(attempt.finishedAt)))
                        setInt(7, attempt.durationSeconds)
                        setDouble(8, attempt.score)
                        setDouble(9, attempt.maxScore)
                        setTimestamp(10, Timestamp.from(now))
                        executeUpdate()
                        generatedKeys.use { keys ->
                            check(keys.next()) { "attempts insert returned no generated key" }
                            keys.getLong(1)
                        }
                    }

                    for (question in attempt.questions) {
                        connection.query(
                            """
                            INSERT INTO attempt_questions (
                                id, attempt_seq, question_id, subject_id, topic_id,
                                question_type, marks, awarded, was_attempted, was_correct
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """.trimIndent(),
                        ) {
                            setObject(1, UUID.randomUUID())
                            setLong(2, seq)
                            setString(3, question.questionId)
                            setString(4, question.subjectId)
                            setString(5, question.topicId)
                            setString(6, question.questionType)
                            setDouble(7, question.marks)
                            setDouble(8, question.awarded)
                            setBoolean(9, question.wasAttempted)
                            setBoolean(10, question.wasCorrect)
                            executeUpdate()
                        }
                    }
                }
                accepted++
            } catch (e: SQLException) {
                if (e.isUniqueViolation()) duplicates += attempt.clientAttemptId else throw e
            }
        }

        return UploadResult(accepted, duplicates, highestSeq = highestSeq(userId))
    }

    fun highestSeq(userId: UUID): Long = database.read { connection ->
        connection.query("SELECT COALESCE(MAX(server_seq), 0) FROM attempts WHERE user_id = ?") {
            setObject(1, userId)
            firstOrNull { it.getLong(1) } ?: 0L
        }
    }

    /**
     * Everything above [since], oldest first, at most [limit] attempts.
     *
     * Questions come back in a second query keyed on the sequences just read,
     * rather than as a join. A join would repeat every attempt row once per
     * question and leave this code reassembling them; two queries is less data
     * over the wire and less to get wrong.
     */
    fun attemptsSince(userId: UUID, since: Long, limit: Int): AttemptPage {
        val attempts = database.read { connection ->
            connection.query(
                """
                SELECT server_seq, client_attempt_id, test_id, test_title,
                       started_at, finished_at, duration_seconds, score, max_score
                FROM attempts
                WHERE user_id = ? AND server_seq > ?
                ORDER BY server_seq
                LIMIT ?
                """.trimIndent(),
            ) {
                setObject(1, userId)
                setLong(2, since)
                setInt(3, limit)
                map { row ->
                    SyncedAttempt(
                        clientAttemptId = row.getString("client_attempt_id"),
                        testId = row.getString("test_id"),
                        testTitle = row.getString("test_title"),
                        startedAt = row.getTimestamp("started_at").time,
                        finishedAt = row.getTimestamp("finished_at").time,
                        durationSeconds = row.getInt("duration_seconds"),
                        score = row.getDouble("score"),
                        maxScore = row.getDouble("max_score"),
                        questions = emptyList(),
                        serverSeq = row.getLong("server_seq"),
                    )
                }
            }
        }

        if (attempts.isEmpty()) return AttemptPage(emptyList(), nextSince = null)

        val bySeq = questionsFor(attempts.mapNotNull { it.serverSeq })
        val filled = attempts.map { it.copy(questions = bySeq[it.serverSeq].orEmpty()) }

        // Only advertise a next page when this one filled up. A short page
        // means the caller is up to date, and saying so stops the client
        // polling for a page it already knows is empty.
        val nextSince = if (filled.size == limit) filled.last().serverSeq else null
        return AttemptPage(filled, nextSince)
    }

    private fun questionsFor(sequences: List<Long>): Map<Long, List<SyncedAttemptQuestion>> {
        if (sequences.isEmpty()) return emptyMap()
        val placeholders = sequences.joinToString(",") { "?" }
        return database.read { connection ->
            connection.query(
                """
                SELECT attempt_seq, question_id, subject_id, topic_id, question_type,
                       marks, awarded, was_attempted, was_correct
                FROM attempt_questions
                WHERE attempt_seq IN ($placeholders)
                ORDER BY attempt_seq, id
                """.trimIndent(),
            ) {
                sequences.forEachIndexed { index, seq -> setLong(index + 1, seq) }
                map { row ->
                    row.getLong("attempt_seq") to SyncedAttemptQuestion(
                        questionId = row.getString("question_id"),
                        subjectId = row.getString("subject_id"),
                        topicId = row.getString("topic_id"),
                        questionType = row.getString("question_type"),
                        marks = row.getDouble("marks"),
                        awarded = row.getDouble("awarded"),
                        wasAttempted = row.getBoolean("was_attempted"),
                        wasCorrect = row.getBoolean("was_correct"),
                    )
                }
            }
        }.groupBy({ it.first }, { it.second })
    }
}
