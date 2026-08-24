package com.gatemaster.app.core.data

import android.content.res.AssetManager
import android.util.Log
import com.gatemaster.app.core.model.Attempt
import com.gatemaster.app.core.model.MockTest
import com.gatemaster.app.core.model.QuestionBankIndex
import com.gatemaster.app.core.model.QuickTestSpec
import com.gatemaster.app.core.model.SubjectQuestionBank
import com.gatemaster.app.core.model.TestSection
import com.gatemaster.app.core.model.TestCatalogue
import com.gatemaster.app.core.model.TestSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/** One finished sitting, kept so the user can see their history. */
@kotlinx.serialization.Serializable
data class AttemptRecord(
    val testId: String,
    val testTitle: String,
    val submittedAtEpochMs: Long,
    val score: Double,
    val maxMarks: Int,
    val correct: Int,
    val incorrect: Int,
    val unattempted: Int,
    val timeTakenMs: Long,
)

/**
 * Loads mock tests from assets and persists attempts to the app's files
 * directory.
 *
 * Files rather than Room, deliberately: it keeps this first working version
 * free of annotation processing, and an attempt is a single self-contained
 * document. Room replaces it when attempt history needs querying for
 * analytics.
 */
class TestRepository(
    private val assets: AssetManager,
    private val filesDir: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val attemptsDir: File get() = File(filesDir, "attempts")
    private val historyFile: File get() = File(filesDir, "attempt_history.json")

    private var cachedCatalogue: List<TestSummary>? = null
    private val testCache = mutableMapOf<String, MockTest>()
    private var cachedBankIndex: QuestionBankIndex? = null
    private val bankCache = mutableMapOf<String, SubjectQuestionBank>()

    suspend fun catalogue(): List<TestSummary> {
        cachedCatalogue?.let { return it }
        return withContext(io) {
            runCatching {
                val raw = assets.open(CATALOGUE_ASSET).bufferedReader().use { it.readText() }
                json.decodeFromString<TestCatalogue>(raw).tests
            }.onFailure {
                Log.e(TAG, "Could not read $CATALOGUE_ASSET", it)
            }.getOrDefault(emptyList()).also { cachedCatalogue = it }
        }
    }

    suspend fun loadTest(testId: String): Result<MockTest> {
        testCache[testId]?.let { return Result.success(it) }
        // A quick test is assembled from the bank rather than read from a file.
        // Recognising it here keeps the player unaware of the difference.
        QuickTestSpec.parse(testId)?.let { return buildQuickTest(it) }
        return withContext(io) {
            runCatching {
                val summary = catalogue().firstOrNull { it.id == testId }
                    ?: error("No test with id $testId")
                val raw = assets.open(summary.file).bufferedReader().use { it.readText() }
                json.decodeFromString<MockTest>(raw)
            }.onSuccess { testCache[testId] = it }
                .onFailure { Log.e(TAG, "Could not load test $testId", it) }
        }
    }

    // -- question bank --------------------------------------------------------

    private suspend fun bankIndex(): QuestionBankIndex {
        cachedBankIndex?.let { return it }
        return withContext(io) {
            runCatching {
                val raw = assets.open(BANK_INDEX_ASSET).bufferedReader().use { it.readText() }
                json.decodeFromString<QuestionBankIndex>(raw)
            }.getOrDefault(QuestionBankIndex()).also { cachedBankIndex = it }
        }
    }

    suspend fun bankFor(subjectId: String): SubjectQuestionBank? {
        bankCache[subjectId]?.let { return it }
        val path = bankIndex().banks[subjectId] ?: return null
        return withContext(io) {
            runCatching {
                val raw = assets.open(path).bufferedReader().use { it.readText() }
                json.decodeFromString<SubjectQuestionBank>(raw)
            }.onFailure { Log.e(TAG, "Could not read question bank $path", it) }
                .getOrNull()
                ?.also { bankCache[subjectId] = it }
        }
    }

    /** Topic ids that have at least [minimum] questions, so the UI can offer a test. */
    suspend fun topicsWithQuestions(subjectId: String, minimum: Int = MIN_TOPIC_QUESTIONS): Set<String> {
        val bank = bankFor(subjectId) ?: return emptySet()
        return bank.questions
            .mapNotNull { it.topicId }
            .groupingBy { it }
            .eachCount()
            .filterValues { it >= minimum }
            .keys
    }

    suspend fun questionCount(subjectId: String, topicId: String? = null): Int {
        val bank = bankFor(subjectId) ?: return 0
        return if (topicId == null) bank.questions.size else bank.forTopic(topicId).size
    }

    /**
     * Assembles a practice test.
     *
     * Questions are shuffled so a second attempt is not the same paper in the
     * same order, and a subject test is capped so it stays a phone-sized
     * session rather than turning back into a mock.
     */
    private suspend fun buildQuickTest(spec: QuickTestSpec): Result<MockTest> {
        val bank = bankFor(spec.subjectId)
            ?: return Result.failure(IllegalStateException("No questions for ${spec.subjectId} yet"))

        val pool = if (spec.topicId != null) bank.forTopic(spec.topicId) else bank.questions
        if (pool.isEmpty()) {
            return Result.failure(IllegalStateException("No questions for this topic yet"))
        }

        val limit = if (spec.topicId != null) MAX_TOPIC_QUESTIONS else MAX_SUBJECT_QUESTIONS
        val chosen = pool.shuffled().take(limit)

        val questions = chosen.mapIndexed { index, q ->
            q.toQuestion(number = index + 1, subjectId = spec.subjectId, topicTitle = q.topicId)
        }

        val title = if (spec.topicId != null) "Topic practice" else "Subject practice"
        val test = MockTest(
            id = spec.id,
            title = title,
            description = "",
            durationMinutes = QuickTestSpec.durationFor(questions.size),
            sections = listOf(
                TestSection(id = "practice", name = title, questionIds = questions.map { it.id }),
            ),
            questions = questions,
        )

        testCache[spec.id] = test
        return Result.success(test)
    }

    // -- in-progress attempts -------------------------------------------------

    suspend fun saveAttempt(attempt: Attempt) = withContext(io) {
        runCatching {
            attemptsDir.mkdirs()
            // Write to a temp file first so a kill mid-write cannot leave a
            // half-written attempt that fails to parse on the next launch.
            val target = attemptFile(attempt.testId)
            val tmp = File(target.absolutePath + ".tmp")
            tmp.writeText(json.encodeToString(attempt))
            if (!tmp.renameTo(target)) {
                target.delete()
                tmp.renameTo(target)
            }
        }.onFailure { Log.e(TAG, "Could not save attempt for ${attempt.testId}", it) }
        Unit
    }

    suspend fun loadAttempt(testId: String): Attempt? = withContext(io) {
        val file = attemptFile(testId)
        if (!file.isFile) return@withContext null
        runCatching { json.decodeFromString<Attempt>(file.readText()) }
            .onFailure {
                Log.e(TAG, "Discarding unreadable attempt for $testId", it)
                file.delete()
            }
            .getOrNull()
    }

    suspend fun clearAttempt(testId: String) = withContext(io) {
        attemptFile(testId).delete()
        Unit
    }

    suspend fun hasAttemptInProgress(testId: String): Boolean = withContext(io) {
        attemptFile(testId).isFile
    }

    // -- completed history ----------------------------------------------------

    suspend fun history(): List<AttemptRecord> = withContext(io) {
        if (!historyFile.isFile) return@withContext emptyList()
        runCatching { json.decodeFromString<List<AttemptRecord>>(historyFile.readText()) }
            .getOrDefault(emptyList())
            .sortedByDescending { it.submittedAtEpochMs }
    }

    suspend fun recordAttempt(record: AttemptRecord) = withContext(io) {
        runCatching {
            val existing = history()
            val updated = (listOf(record) + existing).take(MAX_HISTORY)
            historyFile.writeText(json.encodeToString(updated))
        }.onFailure { Log.e(TAG, "Could not record attempt", it) }
        Unit
    }

    private fun attemptFile(testId: String) =
        File(attemptsDir, testId.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".json")

    private companion object {
        const val TAG = "TestRepository"
        const val CATALOGUE_ASSET = "tests/catalogue.json"
        const val BANK_INDEX_ASSET = "questions/index.json"
        const val MIN_TOPIC_QUESTIONS = 3
        const val MAX_TOPIC_QUESTIONS = 10
        const val MAX_SUBJECT_QUESTIONS = 20
        const val MAX_HISTORY = 100
    }
}
