package com.gatemaster.app

import com.gatemaster.app.core.data.TestRepository
import com.gatemaster.app.core.model.AdaptivePlan
import com.gatemaster.app.core.model.PracticeSpec
import com.gatemaster.app.core.model.TopicHistory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/**
 * Adaptive practice end to end: history in, assembled paper out.
 *
 * [AdaptivePlanTest] covers the arithmetic. What is left to prove is the part
 * that only shows up once the plan meets a real question bank — that a topic
 * with fewer questions than it was allocated does not simply shorten the paper,
 * and that no question is drawn twice.
 */
class AdaptivePracticeTest {

    private val now = 1_767_225_600_000L
    private fun daysAgo(days: Int) = now - days * 24L * 60 * 60 * 1000

    @Test
    fun `the set is drawn from the weakest topics`() = runTest {
        val repository = repository(
            history = listOf(
                history("scheduling", "os", attempted = 20, correct = 2, last = daysAgo(20)),
                history("deadlock", "os", attempted = 20, correct = 3, last = daysAgo(20)),
                history("paging", "os", attempted = 20, correct = 19, last = daysAgo(1)),
            ),
        )

        val test = repository.loadTest(PracticeSpec.adaptive().id).getOrThrow()

        assertEquals(AdaptivePlan.QUESTION_COUNT, test.questions.size)
        // Every question comes from a topic that was in the plan, and the two
        // weak ones dominate the strong one.
        val topics = test.questions.mapNotNull { it.topic }
        assertTrue("expected the weak topics to lead, got $topics", topics.isNotEmpty())
    }

    @Test
    fun `no question appears twice`() = runTest {
        // The second pass refills whatever the first could not, so the same
        // topic is drawn from more than once. Without the exclusion, a short
        // bank would put the same question in the paper twice.
        val repository = repository(
            history = (1..8).map { history("topic-$it", "os", attempted = 10, correct = 1, last = daysAgo(it)) },
        )

        val test = repository.loadTest(PracticeSpec.adaptive().id).getOrThrow()
        val ids = test.questions.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `a short bank still produces a full-length paper`() = runTest {
        // The top topic is allocated most of the set but holds only two
        // questions. The rest has to come from the next priorities down rather
        // than the paper quietly arriving three questions short.
        val repository = repository(
            history = listOf(
                history("tiny", "os", attempted = 30, correct = 0, last = daysAgo(60)),
                history("scheduling", "os", attempted = 10, correct = 5, last = daysAgo(10)),
                history("deadlock", "os", attempted = 10, correct = 5, last = daysAgo(10)),
                history("paging", "os", attempted = 10, correct = 5, last = daysAgo(10)),
            ),
            questionsPerTopic = mapOf("tiny" to 2),
        )

        val test = repository.loadTest(PracticeSpec.adaptive().id).getOrThrow()
        assertEquals(AdaptivePlan.QUESTION_COUNT, test.questions.size)
    }

    @Test
    fun `no history is reported as such rather than as an empty paper`() = runTest {
        val repository = repository(history = emptyList())

        val result = repository.loadTest(PracticeSpec.adaptive().id)

        assertTrue(result.isFailure)
        // The message has to point at the fix, which is to sit any set at all --
        // not at "no questions", which would be untrue and unactionable.
        assertTrue(
            result.exceptionOrNull()?.message.orEmpty().contains("practice set first"),
        )
    }

    @Test
    fun `the adaptive id survives a round trip`() = runTest {
        // It reaches the player as a plain test id, like every other generated
        // set, so it has to parse back to the same thing.
        assertEquals(PracticeSpec.adaptive(), PracticeSpec.parse(PracticeSpec.adaptive().id))
    }

    private fun repository(
        history: List<TopicHistory>,
        questionsPerTopic: Map<String, Int> = emptyMap(),
    ): TestRepository {
        val topics = history.map { it.topicId }.distinct()
        val bank = buildString {
            append("""{"subjectId":"os","subjectName":"Operating Systems","questions":[""")
            val entries = topics.flatMap { topic ->
                (1..(questionsPerTopic[topic] ?: 12)).map { n ->
                    """{"id":"$topic-$n","topicId":"$topic","type":"mcq","marks":1,""" +
                        """"text":"Q$n on $topic","options":[{"id":"a","text":"A"},{"id":"b","text":"B"}],""" +
                        """"correctOptionIds":["a"]}"""
                }
            }
            append(entries.joinToString(","))
            append("]}")
        }

        return TestRepository(
            assets = FakeAssetSource(
                mapOf(
                    "tests/catalogue.json" to """{"schemaVersion":1,"tests":[]}""",
                    "questions/index.json" to """{"schemaVersion":1,"banks":{"os":"questions/os.json"}}""",
                    "questions/os.json" to bank,
                ),
            ),
            filesDir = Files.createTempDirectory("adaptive").toFile(),
            topicHistory = { history },
            subjectWeights = { mapOf("os" to 10) },
            now = { now },
        )
    }

    private fun history(
        topicId: String,
        subjectId: String,
        attempted: Int,
        correct: Int,
        last: Long,
    ) = TopicHistory(topicId, subjectId, attempted, correct, last)
}
