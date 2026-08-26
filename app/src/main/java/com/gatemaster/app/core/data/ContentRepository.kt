package com.gatemaster.app.core.data

import android.util.Log
import com.gatemaster.app.core.model.Branch
import com.gatemaster.app.core.model.ContentIndex
import com.gatemaster.app.core.model.Paper
import com.gatemaster.app.core.model.Subject
import com.gatemaster.app.core.model.Topic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** A topic matched by [ContentRepository.search], with the subject it belongs to. */
data class SearchHit(
    val subject: Subject,
    val topic: Topic,
)

/**
 * Reads the bundled study material.
 *
 * Two things the previous implementation got wrong and this one does not:
 * parsing happens on [Dispatchers.IO] rather than in `onCreate`, and a failure
 * surfaces as [Result.failure] instead of being logged and silently turned into
 * an empty course list.
 */
class ContentRepository(
    private val assets: AssetSource,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val mutex = Mutex()

    @Volatile
    private var cached: ContentIndex? = null

    suspend fun index(): Result<ContentIndex> {
        cached?.let { return Result.success(it) }
        return mutex.withLock {
            cached?.let { return@withLock Result.success(it) }
            runCatching {
                withContext(io) {
                    val raw = assets.open(INDEX_ASSET)
                        .bufferedReader()
                        .use { it.readText() }
                    json.decodeFromString<ContentIndex>(raw).prunedToBundledAssets()
                }
            }.onSuccess { cached = it }
                .onFailure { Log.e(TAG, "Could not read $INDEX_ASSET", it) }
        }
    }

    /**
     * Drops anything the index names but this build does not carry.
     *
     * PDFs are not kept in the repository, so a build made from a plain
     * checkout has the index but not the documents. Pruning here means the app
     * offers what it can actually open, instead of a handout list that errors
     * when tapped. It runs once, behind the same cache as the parse.
     */
    private fun ContentIndex.prunedToBundledAssets(): ContentIndex {
        // The same aptitude articles are shared by all 30 papers, so the index
        // names roughly three times as many paths as it has distinct files.
        val checked = HashMap<String, Boolean>()
        fun bundled(path: String) = checked.getOrPut(path) { assets.exists(path) }

        val prunedBranches = branches.map { branch ->
            val subjects = branch.subjects.map { subject ->
                subject.copy(
                    topics = subject.topics.filter { bundled(it.content.path) },
                    referenceNotes = subject.referenceNotes.filter { bundled(it.content.path) },
                    shortNotes = subject.shortNotes?.takeIf { bundled(it.path) },
                )
            }
            branch.copy(
                subjects = subjects,
                noteCount = subjects.sumOf { it.noteCount },
                hasNotes = subjects.any { !it.isSyllabusOnly },
            )
        }

        val prunedPapers = papers
            .filter { bundled(it.paper.path) }
            .map { paper -> paper.copy(answerKey = paper.answerKey?.takeIf { bundled(it.path) }) }

        return copy(branches = prunedBranches, papers = prunedPapers)
    }

    private suspend fun indexOrEmpty(): ContentIndex =
        index().getOrDefault(ContentIndex.EMPTY)

    // -- branches -------------------------------------------------------------

    suspend fun branches(): List<Branch> = indexOrEmpty().branches.sortedBy { it.order }

    suspend fun branch(branchId: String): Branch? =
        indexOrEmpty().branch(branchId) ?: indexOrEmpty().branch(ContentIndex.DEFAULT_BRANCH)

    // -- subjects -------------------------------------------------------------

    suspend fun subjects(branchId: String): List<Subject> =
        branch(branchId)?.subjects.orEmpty().sortedBy { it.order }

    suspend fun subject(branchId: String, subjectId: String): Subject? =
        subjects(branchId).firstOrNull { it.id == subjectId }

    // -- papers ---------------------------------------------------------------

    suspend fun papers(branchId: String): List<Paper> {
        val index = indexOrEmpty()
        val branch = index.branch(branchId) ?: return emptyList()
        return index.papersFor(branch).sortedByDescending { it.year }
    }

    /**
     * Case-insensitive substring search over topic titles within one branch.
     * Exact prefix matches rank above mid-word ones, so typing "bin" surfaces
     * "Binary Search" before "Combinational Circuits".
     */
    suspend fun search(branchId: String, query: String, limit: Int = 60): List<SearchHit> {
        val q = query.trim()
        if (q.length < MIN_QUERY_LENGTH) return emptyList()

        return withContext(io) {
            val hits = mutableListOf<Pair<Int, SearchHit>>()
            for (subject in subjects(branchId)) {
                for (topic in subject.topics) {
                    val idx = topic.title.indexOf(q, ignoreCase = true)
                    if (idx < 0) continue
                    val startsWord = idx == 0 || !topic.title[idx - 1].isLetterOrDigit()
                    val rank = if (idx == 0) 0 else if (startsWord) 1 else 2
                    hits += rank to SearchHit(subject, topic)
                }
            }
            hits.sortedWith(compareBy({ it.first }, { it.second.topic.title.length }))
                .take(limit)
                .map { it.second }
        }
    }

    private companion object {
        const val TAG = "ContentRepository"
        const val INDEX_ASSET = "content_index.json"
        const val MIN_QUERY_LENGTH = 2
    }
}
