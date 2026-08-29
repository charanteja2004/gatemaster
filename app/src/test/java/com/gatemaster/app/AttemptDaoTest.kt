package com.gatemaster.app

import androidx.room.Room
import com.gatemaster.app.core.data.db.AttemptDao
import com.gatemaster.app.core.data.db.AttemptEntity
import com.gatemaster.app.core.data.db.GateMasterDatabase
import com.gatemaster.app.core.data.db.QuestionResultEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The analytics queries.
 *
 * These aggregates are the reason the attempt history moved into a database,
 * so they are worth testing directly: an ORDER BY the wrong way round or a
 * forgotten HAVING would produce a plausible screen full of wrong advice.
 *
 * Robolectric runs them on the JVM, so they stay in the same suite as
 * everything else and need no emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AttemptDaoTest {

    private lateinit var db: GateMasterDatabase
    private lateinit var dao: AttemptDao

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GateMasterDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.attemptDao()
    }

    @After
    fun close() = db.close()

    private fun attempt(
        testId: String = "practice:mixed:all",
        at: Long = 1_000,
        score: Double = 10.0,
        maxMarks: Int = 20,
        correct: Int = 5,
        incorrect: Int = 2,
        unattempted: Int = 3,
        timeTakenMs: Long = 60_000,
    ) = AttemptEntity(
        testId = testId,
        title = "Mixed test",
        submittedAtEpochMs = at,
        score = score,
        maxMarks = maxMarks,
        correct = correct,
        incorrect = incorrect,
        unattempted = unattempted,
        timeTakenMs = timeTakenMs,
    )

    private fun result(
        questionId: String,
        subjectId: String?,
        topicId: String?,
        kind: String,
        marks: Int = 1,
        awarded: Double = if (kind == "CORRECT") marks.toDouble() else 0.0,
    ) = QuestionResultEntity(
        attemptId = 0,
        questionId = questionId,
        subjectId = subjectId,
        topicId = topicId,
        marks = marks,
        marksAwarded = awarded,
        kind = kind,
    )

    // -- the recommendation gate ----------------------------------------------

    @Test
    fun `the practised-topic count follows what is recorded`() = runTest {
        // This is the regression. The count gates the recommended set on the
        // Tests tab, and the thing that changes it -- submitting a paper --
        // happens on a different screen. It was read once when the screen
        // loaded, so the card stayed hidden after the very paper that unlocked
        // it. A Flow is the fix; this is what proves it emits.
        assertEquals(0, dao.practisedTopicCount().first())

        dao.record(
            attempt = attempt(),
            results = listOf(
                result("q1", "os", "scheduling", "CORRECT"),
                result("q2", "os", "deadlock", "INCORRECT"),
            ),
        )
        assertEquals(2, dao.practisedTopicCount().first())

        dao.record(
            attempt = attempt(testId = "practice:subject:dbms"),
            results = listOf(result("q3", "dbms", "joins", "CORRECT")),
        )
        assertEquals(3, dao.practisedTopicCount().first())
    }

    @Test
    fun `a topic answered twice is still one topic`() = runTest {
        dao.record(
            attempt = attempt(),
            results = listOf(
                result("q1", "os", "scheduling", "CORRECT"),
                result("q2", "os", "scheduling", "INCORRECT"),
            ),
        )
        assertEquals(1, dao.practisedTopicCount().first())
    }

    @Test
    fun `questions left blank do not count as a practised topic`() = runTest {
        // Running out of time is not the same as having practised something,
        // and counting it would unlock a recommendation drawn from topics the
        // user has never actually answered.
        dao.record(
            attempt = attempt(),
            results = listOf(
                result("q1", "os", "scheduling", "UNATTEMPTED"),
                result("q2", "os", "deadlock", "UNATTEMPTED"),
            ),
        )
        assertEquals(0, dao.practisedTopicCount().first())
    }

    @Test
    fun `a question with no topic does not count`() = runTest {
        // Bundled tests carry no topic ids, so their questions cannot tell the
        // scheduler anything and must not gate it either.
        dao.record(
            attempt = attempt(),
            results = listOf(result("q1", "os", null, "CORRECT")),
        )
        assertEquals(0, dao.practisedTopicCount().first())
    }

    // -- recording ------------------------------------------------------------

    @Test
    fun `an attempt and its questions are stored together`() = runTest {
        dao.record(
            attempt(),
            listOf(
                result("q1", "algo", "algo_sort", "CORRECT"),
                result("q2", "algo", "algo_sort", "INCORRECT"),
            ),
        )

        assertEquals(1, dao.count())
        assertEquals(1, dao.recentAttempts().first().size)
    }

    @Test
    fun `deleting an attempt takes its questions with it`() = runTest {
        dao.record(attempt(), listOf(result("q1", "algo", "algo_sort", "CORRECT")))

        dao.clear()

        assertEquals(0, dao.count())
        assertEquals(emptyList<Any>(), dao.subjectAccuracy().first())
    }

    // -- totals ---------------------------------------------------------------

    @Test
    fun `totals add up across attempts`() = runTest {
        dao.record(attempt(correct = 5, incorrect = 2, unattempted = 3), emptyList())
        dao.record(attempt(at = 2_000, correct = 7, incorrect = 1, unattempted = 2), emptyList())

        val totals = dao.totals().first()

        assertEquals(2, totals.attempts)
        assertEquals(12, totals.correct)
        assertEquals(3, totals.incorrect)
        assertEquals(15, totals.attemptedQuestions)
        assertEquals(12f / 15, totals.accuracy, 1e-6f)
    }

    @Test
    fun `totals on an empty history are zero rather than null`() = runTest {
        val totals = dao.totals().first()

        assertEquals(0, totals.attempts)
        assertEquals(0f, totals.accuracy, 1e-6f)
    }

    // -- subject accuracy -----------------------------------------------------

    @Test
    fun `subject accuracy ignores unattempted questions`() = runTest {
        dao.record(
            attempt(),
            listOf(
                result("q1", "algo", "algo_sort", "CORRECT"),
                result("q2", "algo", "algo_sort", "INCORRECT"),
                result("q3", "algo", "algo_sort", "UNATTEMPTED"),
            ),
        )

        val algo = dao.subjectAccuracy().first().single()

        // Two answered, one right: blanks would drag this to a third and
        // punish running out of time rather than not knowing the subject.
        assertEquals(2, algo.attempted)
        assertEquals(0.5f, algo.accuracy, 1e-6f)
    }

    @Test
    fun `subject accuracy is reported weakest first`() = runTest {
        dao.record(
            attempt(),
            listOf(
                result("a1", "algo", "t1", "CORRECT"),
                result("a2", "algo", "t1", "CORRECT"),
                result("d1", "dbms", "t2", "INCORRECT"),
                result("d2", "dbms", "t2", "INCORRECT"),
                result("o1", "os", "t3", "CORRECT"),
                result("o2", "os", "t3", "INCORRECT"),
            ),
        )

        val order = dao.subjectAccuracy().first().map { it.subjectId }

        assertEquals(listOf("dbms", "os", "algo"), order)
    }

    @Test
    fun `a question with no subject is left out of the subject breakdown`() = runTest {
        dao.record(
            attempt(),
            listOf(
                result("q1", null, null, "CORRECT"),
                result("q2", "algo", "algo_sort", "CORRECT"),
            ),
        )

        assertEquals(listOf("algo"), dao.subjectAccuracy().first().map { it.subjectId })
    }

    // -- weak topics ----------------------------------------------------------

    @Test
    fun `weakest topics are ordered worst first`() = runTest {
        dao.record(
            attempt(),
            listOf(
                result("s1", "algo", "sorting", "CORRECT"),
                result("s2", "algo", "sorting", "CORRECT"),
                result("s3", "algo", "sorting", "CORRECT"),
                result("g1", "algo", "graphs", "INCORRECT"),
                result("g2", "algo", "graphs", "INCORRECT"),
                result("g3", "algo", "graphs", "CORRECT"),
            ),
        )

        val topics = dao.weakestTopics(minAttempted = 3, limit = 8).first()

        assertEquals(listOf("graphs", "sorting"), topics.map { it.topicId })
        assertEquals(1f / 3, topics.first().accuracy, 1e-6f)
    }

    @Test
    fun `a topic answered once is not called a weakness`() = runTest {
        dao.record(
            attempt(),
            listOf(
                result("h1", "algo", "hashing", "INCORRECT"),
                result("s1", "algo", "sorting", "CORRECT"),
                result("s2", "algo", "sorting", "INCORRECT"),
                result("s3", "algo", "sorting", "INCORRECT"),
            ),
        )

        val topics = dao.weakestTopics(minAttempted = 3, limit = 8).first()

        assertEquals(
            "one unlucky question is not evidence of a weak topic",
            listOf("sorting"),
            topics.map { it.topicId },
        )
    }

    @Test
    fun `weak topics accumulate across attempts`() = runTest {
        val row = { id: String, kind: String -> result(id, "algo", "graphs", kind) }
        dao.record(attempt(), listOf(row("g1", "INCORRECT"), row("g2", "INCORRECT")))
        dao.record(attempt(at = 2_000), listOf(row("g3", "CORRECT")))

        val graphs = dao.weakestTopics(minAttempted = 3, limit = 8).first().single()

        assertEquals(3, graphs.attempted)
        assertEquals(1, graphs.correct)
    }

    // -- trend ----------------------------------------------------------------

    @Test
    fun `the score trend runs oldest first`() = runTest {
        dao.record(attempt(at = 3_000, score = 15.0), emptyList())
        dao.record(attempt(at = 1_000, score = 5.0), emptyList())
        dao.record(attempt(at = 2_000, score = 10.0), emptyList())

        val trend = dao.scoreTrend().first()

        assertEquals(listOf(1_000L, 2_000L, 3_000L), trend.map { it.submittedAtEpochMs })
        assertEquals(0.25f, trend.first().percent, 1e-6f)
    }

    @Test
    fun `recent attempts run newest first`() = runTest {
        dao.record(attempt(at = 1_000, testId = "old"), emptyList())
        dao.record(attempt(at = 2_000, testId = "new"), emptyList())

        assertEquals(listOf("new", "old"), dao.recentAttempts().first().map { it.testId })
    }

    @Test
    fun `a paper out of marks does not divide by zero`() = runTest {
        dao.record(attempt(score = 0.0, maxMarks = 0), emptyList())

        assertTrue(dao.scoreTrend().first().single().percent == 0f)
    }
}
