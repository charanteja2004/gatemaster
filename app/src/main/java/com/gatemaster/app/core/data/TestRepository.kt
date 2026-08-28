package com.gatemaster.app.core.data

import android.util.Log
import com.gatemaster.app.core.model.Attempt
import com.gatemaster.app.core.model.BankQuestion
import com.gatemaster.app.core.model.MockTest
import com.gatemaster.app.core.model.PracticeMode
import com.gatemaster.app.core.model.AdaptivePlan
import com.gatemaster.app.core.model.PracticeSpec
import com.gatemaster.app.core.model.TopicHistory
import com.gatemaster.app.core.model.Question
import com.gatemaster.app.core.model.QuestionBankIndex
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
    private val assets: AssetSource,
    private val filesDir: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    /**
     * What the attempt history says about each topic, for adaptive practice.
     *
     * A lambda rather than the DAO, so this class stays free of Room and stays
     * constructible in a test with no database -- which every existing test
     * here relies on. Empty by default, and an adaptive set built from nothing
     * simply reports that there is no history yet.
     */
    private val topicHistory: suspend () -> List<TopicHistory> = { emptyList() },
    /** Subject id to its marks in the paper, so weightage can bias the draw. */
    private val subjectWeights: suspend () -> Map<String, Int> = { emptyMap() },
    private val now: () -> Long = System::currentTimeMillis,
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
        // A practice test is assembled from the bank rather than read from a
        // file. Recognising it here keeps the player unaware of the difference.
        PracticeSpec.parse(testId)?.let { spec ->
            return buildPracticeTest(spec, requestedId = testId)
                .onSuccess { testCache[testId] = it }
        }
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

    /** Subject ids that have a bank at all, in the order the index lists them. */
    suspend fun subjectsWithBanks(): List<String> = bankIndex().banks.keys.toList()

    /** Topic ids that have at least [minimum] questions, so the UI can offer a test. */
    suspend fun topicsWithQuestions(subjectId: String, minimum: Int = MIN_TOPIC_QUESTIONS): Set<String> =
        topicQuestionCounts(subjectId).filterValues { it >= minimum }.keys

    /** How many questions each topic of a subject holds, for the practice tab. */
    suspend fun topicQuestionCounts(subjectId: String): Map<String, Int> =
        bankFor(subjectId)?.countByTopic().orEmpty()

    suspend fun questionCount(subjectId: String, topicId: String? = null): Int {
        val bank = bankFor(subjectId) ?: return 0
        return if (topicId == null) bank.questions.size else bank.forTopic(topicId).size
    }

    // -- practice assembly ----------------------------------------------------

    /** One section of a paper being assembled, before ids and numbers are set. */
    private class Draft(
        val name: String,
        val subjectId: String,
        val questions: List<BankQuestion>,
    )

    /**
     * Assembles a practice paper on demand.
     *
     * Two rules run through all three modes. Questions are shuffled, so a
     * second attempt is not the same paper in the same order. And the draw is
     * balanced across whatever the set spans — topics within a subject,
     * subjects within a mix — so a test cannot quietly become twenty questions
     * about the one topic that happens to have the most written for it.
     */
    private suspend fun buildPracticeTest(
        spec: PracticeSpec,
        requestedId: String,
    ): Result<MockTest> {
        val drafts = when (spec.mode) {
            PracticeMode.TOPIC -> topicDraft(spec)
            PracticeMode.SUBJECT -> subjectDraft(spec)
            PracticeMode.MIXED -> mixedDraft(spec)
            PracticeMode.ADAPTIVE -> adaptiveDraft(spec)
        }.filter { it.questions.isNotEmpty() }

        if (drafts.isEmpty()) {
            return Result.failure(IllegalStateException(emptyMessage(spec.mode)))
        }
        return Result.success(assemble(spec, requestedId, drafts))
    }

    private suspend fun topicDraft(spec: PracticeSpec): List<Draft> {
        val subjectId = spec.subjectIds.firstOrNull() ?: return emptyList()
        val topicId = spec.topicId ?: return emptyList()
        val bank = bankFor(subjectId) ?: return emptyList()
        val chosen = bank.forTopic(topicId).shuffled().take(spec.mode.questionLimit)
        return listOf(Draft(spec.mode.label, subjectId, chosen))
    }

    private suspend fun subjectDraft(spec: PracticeSpec): List<Draft> {
        val subjectId = spec.subjectIds.firstOrNull() ?: return emptyList()
        val bank = bankFor(subjectId) ?: return emptyList()
        val chosen = drawBalanced(bank.byTopic(), spec.mode.questionLimit)
        return listOf(Draft(bank.displayName, subjectId, chosen))
    }

    /**
     * A set drawn from what the history says is weakest and most overdue.
     *
     * The scheduler decides how many questions each topic is worth; this only
     * has to honour that and cope with a topic whose bank cannot fill its
     * share. Anything short is made up from the remaining priorities in order,
     * so a full-length set still comes out when the top topic has three
     * questions written for it.
     */
    private suspend fun adaptiveDraft(spec: PracticeSpec): List<Draft> {
        val history = topicHistory()
        if (history.isEmpty()) return emptyList()

        val ranked = AdaptivePlan.prioritise(history, subjectWeights(), now())
        val allocation = AdaptivePlan.allocate(ranked, spec.mode.questionLimit)
        if (allocation.isEmpty()) return emptyList()

        val used = mutableSetOf<String>()
        val bySubject = linkedMapOf<String, MutableList<BankQuestion>>()

        suspend fun draw(topicId: String, subjectId: String, want: Int): Int {
            if (want <= 0) return 0
            val bank = bankFor(subjectId) ?: return 0
            val taken = bank.forTopic(topicId)
                .filter { it.id !in used }
                .shuffled()
                .take(want)
            if (taken.isEmpty()) return 0
            used += taken.map { it.id }
            bySubject.getOrPut(subjectId) { mutableListOf() } += taken
            return taken.size
        }

        var shortfall = 0
        for (topic in ranked) {
            val want = allocation[topic.topicId] ?: continue
            shortfall += want - draw(topic.topicId, topic.subjectId, want)
        }

        // Second pass for whatever the first could not fill, in priority order
        // and skipping the questions already drawn.
        for (topic in ranked) {
            if (shortfall <= 0) break
            shortfall -= draw(topic.topicId, topic.subjectId, shortfall)
        }

        return bySubject.map { (subjectId, questions) ->
            Draft(bankFor(subjectId)?.displayName ?: subjectId, subjectId, questions)
        }
    }

    /**
     * A mix gets one section per subject, which is the whole point of it: the
     * scorecard then reports a score per subject, and "which subject is
     * costing me marks" is a question a single-subject test cannot answer.
     */
    private suspend fun mixedDraft(spec: PracticeSpec): List<Draft> {
        val subjectIds = spec.subjectIds.ifEmpty { subjectsWithBanks() }
        val banks = subjectIds.mapNotNull { bankFor(it) }.filter { it.questions.isNotEmpty() }
        if (banks.isEmpty()) return emptyList()

        // Balance within each subject first, then across subjects, so a mix is
        // not three-quarters whichever subject has the biggest bank.
        val perSubject = banks.map { drawBalanced(it.byTopic(), spec.mode.questionLimit) }
        val chosen = drawBalanced(perSubject, spec.mode.questionLimit).toSet()

        return banks.mapIndexed { index, bank ->
            Draft(bank.displayName, bank.subjectId, perSubject[index].filter { it in chosen })
        }
    }

    /**
     * Takes up to [limit] items, one from each pool in turn, so the result is
     * spread across the pools rather than dominated by the largest.
     */
    private fun <T> drawBalanced(pools: List<List<T>>, limit: Int): List<T> {
        val queues = pools.map { ArrayDeque(it.shuffled()) }
            .filter { it.isNotEmpty() }
            .toMutableList()

        val taken = mutableListOf<T>()
        var index = 0
        while (taken.size < limit && queues.isNotEmpty()) {
            if (index >= queues.size) index = 0
            val queue = queues[index]
            taken += queue.removeFirst()
            // Dropping the exhausted pool shifts the next one into its place,
            // so the cursor only advances when nothing was removed.
            if (queue.isEmpty()) queues.removeAt(index) else index++
        }
        return taken
    }

    private fun assemble(spec: PracticeSpec, id: String, drafts: List<Draft>): MockTest {
        var number = 0
        val sections = mutableListOf<TestSection>()
        val questions = mutableListOf<Question>()

        drafts.forEach { draft ->
            val converted = draft.questions.map { question ->
                question.toQuestion(
                    number = ++number,
                    subjectId = draft.subjectId,
                    topicTitle = question.topicId,
                )
            }
            questions += converted
            sections += TestSection(
                id = draft.subjectId,
                name = draft.name,
                questionIds = converted.map { it.id },
            )
        }

        return MockTest(
            id = id,
            title = when (spec.mode) {
                PracticeMode.SUBJECT -> "${drafts.first().name} practice"
                else -> spec.mode.label
            },
            description = when (spec.mode) {
                PracticeMode.MIXED -> "Drawn from ${drafts.size} subjects"
                else -> ""
            },
            durationMinutes = PracticeSpec.durationFor(questions.size),
            sections = sections,
            questions = questions,
        )
    }

    private fun emptyMessage(mode: PracticeMode): String = when (mode) {
        PracticeMode.TOPIC -> "No questions for this topic yet"
        PracticeMode.SUBJECT -> "No questions for this subject yet"
        PracticeMode.MIXED -> "No questions for these subjects yet"
        // Not "no questions": there are plenty. There is no history to choose
        // from, which is a different thing and has a different fix.
        PracticeMode.ADAPTIVE -> "Sit a practice set first, then this picks what to revise"
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
        const val MAX_HISTORY = 100
    }
}
