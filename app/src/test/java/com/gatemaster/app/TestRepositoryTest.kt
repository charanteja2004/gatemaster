package com.gatemaster.app

import com.gatemaster.app.core.data.AttemptRecord
import com.gatemaster.app.core.data.TestRepository
import com.gatemaster.app.core.model.AnswerState
import com.gatemaster.app.core.model.Attempt
import com.gatemaster.app.core.model.PracticeSpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Practice-test assembly and attempt persistence.
 *
 * The assembly rules are this app's answer to "a three-hour mock is the wrong
 * shape for a phone", so they are worth pinning down: get the caps wrong and a
 * ten-question topic practice quietly turns back into a full paper, and get the
 * draw wrong and a subject test becomes twenty questions about whichever topic
 * happens to have the most written for it.
 */
class TestRepositoryTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun bankJson(
        subjectId: String,
        subjectName: String,
        vararg topicCounts: Pair<String, Int>,
    ): String {
        val questions = topicCounts.flatMap { (topic, count) ->
            (1..count).map { n ->
                """
                {
                  "id": "$topic-$n",
                  "topicId": "$topic",
                  "type": "mcq",
                  "marks": 1,
                  "text": "Question $n on $topic",
                  "options": [
                    {"id": "A", "text": "a"}, {"id": "B", "text": "b"},
                    {"id": "C", "text": "c"}, {"id": "D", "text": "d"}
                  ],
                  "correctOptionIds": ["A"]
                }
                """.trimIndent()
            }
        }
        return """
            {
              "subjectId": "$subjectId",
              "subjectName": "$subjectName",
              "questions": [${questions.joinToString(",")}]
            }
        """.trimIndent()
    }

    private val defaultAssets = mapOf(
        "questions/index.json" to """
            {
              "schemaVersion": 1,
              "banks": {
                "algo": "questions/algo.json",
                "os": "questions/os.json",
                "dbms": "questions/dbms.json"
              }
            }
        """.trimIndent(),
        "questions/algo.json" to
            bankJson("algo", "Algorithms", "sorting" to 25, "graphs" to 4, "hashing" to 2),
        "questions/os.json" to
            bankJson("os", "Operating Systems", "scheduling" to 12, "memory" to 8),
        "questions/dbms.json" to bankJson("dbms", "Databases", "sql" to 6),
    )

    private fun repository(
        assets: Map<String, String> = defaultAssets,
        filesDir: File = temp.newFolder(),
    ) = TestRepository(FakeAssetSource(assets), filesDir)

    private fun topicIdsOf(prefix: String, ids: List<String>) = ids.count { it.startsWith("$prefix-") }

    // -- topic practice -------------------------------------------------------

    @Test
    fun `topic practice is capped at ten questions`() = runTest {
        val test = repository().loadTest(PracticeSpec.topic("algo", "sorting").id).getOrThrow()

        assertEquals(10, test.questionCount)
        assertTrue(
            "every question should come from the topic that was asked for",
            test.questions.all { it.id.startsWith("sorting-") },
        )
    }

    @Test
    fun `a topic with fewer questions than the cap uses all of them`() = runTest {
        val test = repository().loadTest(PracticeSpec.topic("algo", "graphs").id).getOrThrow()

        assertEquals(4, test.questionCount)
    }

    @Test
    fun `a topic with no questions fails rather than handing back an empty test`() = runTest {
        assertTrue(repository().loadTest(PracticeSpec.topic("algo", "nothing").id).isFailure)
    }

    @Test
    fun `only topics with at least three questions are offered a test`() = runTest {
        val offered = repository().topicsWithQuestions("algo")

        assertTrue("sorting has 25", "sorting" in offered)
        assertTrue("graphs has 4", "graphs" in offered)
        assertFalse("hashing has 2, too few to be worth offering", "hashing" in offered)
    }

    // -- subject practice -----------------------------------------------------

    @Test
    fun `subject practice is capped at twenty`() = runTest {
        val test = repository().loadTest(PracticeSpec.subject("algo").id).getOrThrow()

        assertEquals(20, test.questionCount)
    }

    @Test
    fun `subject practice is spread across topics, not dominated by the biggest`() = runTest {
        val ids = repository().loadTest(PracticeSpec.subject("algo").id).getOrThrow()
            .questions.map { it.id }

        // Both small topics get in whole; sorting fills what is left rather
        // than taking all twenty because it has the most questions written.
        assertEquals(2, topicIdsOf("hashing", ids))
        assertEquals(4, topicIdsOf("graphs", ids))
        assertEquals(14, topicIdsOf("sorting", ids))
    }

    @Test
    fun `subject practice is titled with the subject name`() = runTest {
        val test = repository().loadTest(PracticeSpec.subject("dbms").id).getOrThrow()

        assertEquals("Databases practice", test.title)
    }

    @Test
    fun `a subject with no bank fails rather than handing back an empty test`() = runTest {
        assertTrue(
            "a missing bank should be a failure, not a paper with no questions",
            repository().loadTest(PracticeSpec.subject("cn").id).isFailure,
        )
    }

    // -- mixed tests ----------------------------------------------------------

    @Test
    fun `a mixed test is capped at thirty questions`() = runTest {
        val test = repository().loadTest(PracticeSpec.mixed().id).getOrThrow()

        assertEquals(30, test.questionCount)
    }

    @Test
    fun `a mixed test gets one section per subject, named for the subject`() = runTest {
        val test = repository().loadTest(PracticeSpec.mixed().id).getOrThrow()

        assertEquals(
            listOf("Algorithms", "Operating Systems", "Databases"),
            test.sections.map { it.name },
        )
    }

    @Test
    fun `a mixed test is drawn evenly across subjects`() = runTest {
        val test = repository().loadTest(PracticeSpec.mixed().id).getOrThrow()
        val sizes = test.sections.associate { it.id to it.questionIds.size }

        // Databases only holds six, so it contributes all six; the shortfall is
        // shared evenly by the two subjects that have more to give rather than
        // falling entirely on whichever bank happens to be largest.
        assertEquals(6, sizes["dbms"])
        assertEquals(sizes["algo"], sizes["os"])
        assertEquals(30, sizes.values.sum())
    }

    @Test
    fun `a mixed test numbers its questions straight through the sections`() = runTest {
        val test = repository().loadTest(PracticeSpec.mixed().id).getOrThrow()

        assertEquals((1..30).toList(), test.orderedQuestions.map { it.number })
    }

    @Test
    fun `a mixed test can be narrowed to chosen subjects`() = runTest {
        val test = repository()
            .loadTest(PracticeSpec.mixed(listOf("algo", "dbms")).id)
            .getOrThrow()

        assertEquals(listOf("algo", "dbms"), test.sections.map { it.id })
    }

    @Test
    fun `a mixed test ignores a chosen subject that has no bank`() = runTest {
        val test = repository()
            .loadTest(PracticeSpec.mixed(listOf("algo", "cn")).id)
            .getOrThrow()

        assertEquals(listOf("algo"), test.sections.map { it.id })
    }

    @Test
    fun `a mixed test with nothing behind it fails`() = runTest {
        val repo = repository(
            assets = mapOf("questions/index.json" to """{"schemaVersion": 1, "banks": {}}"""),
        )

        assertTrue(repo.loadTest(PracticeSpec.mixed().id).isFailure)
    }

    @Test
    fun `every subject with a bank is offered to the mixer`() = runTest {
        assertEquals(listOf("algo", "os", "dbms"), repository().subjectsWithBanks())
    }

    // -- shared assembly rules ------------------------------------------------

    @Test
    fun `duration follows the two minutes a question pace`() = runTest {
        val repo = repository()
        assertEquals(20, repo.loadTest(PracticeSpec.topic("algo", "sorting").id).getOrThrow().durationMinutes)
        assertEquals(60, repo.loadTest(PracticeSpec.mixed().id).getOrThrow().durationMinutes)
        // Four questions is eight minutes, still above the five-minute floor.
        assertEquals(8, repo.loadTest(PracticeSpec.topic("algo", "graphs").id).getOrThrow().durationMinutes)
    }

    @Test
    fun `assembled questions carry the subject they came from`() = runTest {
        val test = repository().loadTest(PracticeSpec.mixed().id).getOrThrow()

        assertEquals(
            setOf("algo", "os", "dbms"),
            test.questions.mapNotNull { it.subjectId }.toSet(),
        )
    }

    @Test
    fun `question counts are reported per subject and per topic`() = runTest {
        val repo = repository()

        assertEquals(31, repo.questionCount("algo"))
        assertEquals(25, repo.questionCount("algo", "sorting"))
        assertEquals(0, repo.questionCount("algo", "nonexistent"))
        assertEquals(0, repo.questionCount("cn"))
        assertEquals(mapOf("sorting" to 25, "graphs" to 4, "hashing" to 2), repo.topicQuestionCounts("algo"))
    }

    @Test
    fun `a malformed bank reads as no questions rather than crashing`() = runTest {
        val repo = repository(
            assets = mapOf(
                "questions/index.json" to
                    """{"schemaVersion": 1, "banks": {"algo": "questions/algo.json"}}""",
                "questions/algo.json" to "{ this is not json",
            ),
        )

        assertEquals(0, repo.questionCount("algo"))
        assertTrue(repo.loadTest(PracticeSpec.subject("algo").id).isFailure)
    }

    @Test
    fun `a test id written by an older version still resolves`() = runTest {
        // A saved attempt outlives the id scheme that created it.
        val repo = repository()

        assertEquals(10, repo.loadTest("quick:algo:sorting").getOrThrow().questionCount)
        assertEquals(20, repo.loadTest("quick:algo").getOrThrow().questionCount)
    }

    @Test
    fun `a legacy id keeps its own identity so its saved attempt still matches`() = runTest {
        assertEquals("quick:algo", repository().loadTest("quick:algo").getOrThrow().id)
    }

    // -- attempts -------------------------------------------------------------

    @Test
    fun `an in-progress attempt survives a round trip`() = runTest {
        val repo = repository()
        val testId = PracticeSpec.topic("algo", "sorting").id
        val attempt = Attempt(
            testId = testId,
            startedAtEpochMs = 1_000,
            durationMinutes = 20,
            answers = mapOf(
                "sorting-1" to AnswerState("sorting-1", selectedOptionIds = setOf("A")),
            ),
        )

        repo.saveAttempt(attempt)

        assertTrue(repo.hasAttemptInProgress(testId))
        val loaded = repo.loadAttempt(testId)
        assertEquals(attempt.answers, loaded?.answers)
        assertEquals(attempt.startedAtEpochMs, loaded?.startedAtEpochMs)

        repo.clearAttempt(testId)

        assertFalse(repo.hasAttemptInProgress(testId))
        assertNull(repo.loadAttempt(testId))
    }

    @Test
    fun `an unreadable attempt is discarded instead of failing every launch`() = runTest {
        val filesDir = temp.newFolder()
        val repo = repository(filesDir = filesDir)
        repo.saveAttempt(Attempt("t1", 1, 20))

        File(filesDir, "attempts/t1.json").writeText("half a written file")

        assertNull(repo.loadAttempt("t1"))
        assertFalse(
            "the bad file should be cleared, not left to fail again tomorrow",
            repo.hasAttemptInProgress("t1"),
        )
    }

    @Test
    fun `history is newest first and bounded`() = runTest {
        val repo = repository()
        repeat(105) { n ->
            repo.recordAttempt(
                AttemptRecord(
                    testId = "t$n",
                    testTitle = "Test $n",
                    submittedAtEpochMs = n.toLong(),
                    score = n.toDouble(),
                    maxMarks = 10,
                    correct = 1,
                    incorrect = 0,
                    unattempted = 0,
                    timeTakenMs = 60_000,
                ),
            )
        }

        val history = repo.history()

        assertEquals(100, history.size)
        assertEquals("t104", history.first().testId)
        assertEquals(
            history.map { it.submittedAtEpochMs }.sortedDescending(),
            history.map { it.submittedAtEpochMs },
        )
    }

    @Test
    fun `history starts empty rather than throwing on a fresh install`() = runTest {
        assertEquals(emptyList<AttemptRecord>(), repository().history())
    }
}
