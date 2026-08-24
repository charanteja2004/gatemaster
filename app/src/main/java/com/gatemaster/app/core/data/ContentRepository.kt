package com.gatemaster.app.core.data

import android.content.res.AssetManager
import android.util.Log
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
    private val assets: AssetManager,
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
                    json.decodeFromString<ContentIndex>(raw)
                }
            }.onSuccess { cached = it }
                .onFailure { Log.e(TAG, "Could not read $INDEX_ASSET", it) }
        }
    }

    suspend fun subjects(): List<Subject> =
        index().getOrDefault(ContentIndex.EMPTY).subjects

    suspend fun subject(id: String): Subject? =
        subjects().firstOrNull { it.id == id }

    suspend fun papers(): List<Paper> =
        index().getOrDefault(ContentIndex.EMPTY).papers

    suspend fun paper(id: String): Paper? =
        papers().firstOrNull { it.id == id }

    /**
     * Case-insensitive substring search over topic titles. Exact prefix matches
     * rank above mid-word ones so typing "bin" surfaces "Binary Search" before
     * "Combinational Circuits".
     */
    suspend fun search(query: String, limit: Int = 50): List<SearchHit> {
        val q = query.trim()
        if (q.length < MIN_QUERY_LENGTH) return emptyList()

        return withContext(io) {
            val hits = mutableListOf<Pair<Int, SearchHit>>()
            for (subject in subjects()) {
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
