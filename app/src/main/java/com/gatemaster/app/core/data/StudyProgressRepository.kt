package com.gatemaster.app.core.data

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** What the app remembers about one topic the reader has opened. */
@Serializable
data class TopicProgress(
    val topicId: String,
    val branchId: String,
    val subjectId: String,
    val subjectName: String,
    val title: String,
    val path: String,
    val isPdf: Boolean = false,
    val lastOpenedEpochMs: Long = 0,
    /** How far down the article the reader got, 0f..1f. */
    val furthest: Float = 0f,
    val bookmarked: Boolean = false,
) {
    /**
     * Reaching the last tenth counts as read. Demanding a full 100% would mean
     * almost nothing ever gets ticked off — the footer and the next/previous
     * bar sit below the last paragraph.
     */
    val isRead: Boolean get() = furthest >= READ_THRESHOLD

    companion object {
        const val READ_THRESHOLD = 0.9f
    }
}

@Serializable
private data class ProgressFile(
    val version: Int = 1,
    val topics: Map<String, TopicProgress> = emptyMap(),
)

/**
 * Remembers what has been read, how far, and what was saved.
 *
 * Backed by a single JSON file rather than Room: it is one small document, read
 * once at startup and held in memory, and every screen wants the whole thing.
 * Room earns its place when this needs querying for analytics.
 */
class StudyProgressRepository(
    private val filesDir: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    /** Injectable so tests can order events without sleeping. */
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val mutex = Mutex()
    private val file: File get() = File(filesDir, "study_progress.json")

    private val _progress = MutableStateFlow<Map<String, TopicProgress>>(emptyMap())

    /** Every topic the reader has opened, keyed by topic id. */
    val progress: StateFlow<Map<String, TopicProgress>> = _progress.asStateFlow()

    private var loaded = false

    suspend fun load() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            val stored = withContext(io) {
                if (!file.isFile) return@withContext emptyMap()
                runCatching { json.decodeFromString<ProgressFile>(file.readText()).topics }
                    .onFailure {
                        Log.e(TAG, "Discarding unreadable progress file", it)
                        file.delete()
                    }
                    .getOrDefault(emptyMap())
            }
            _progress.value = stored
            loaded = true
        }
    }

    /** Called when a topic is opened. Keeps any progress already recorded. */
    suspend fun recordOpen(entry: TopicProgress) = update(entry.topicId) { existing ->
        (existing ?: entry).copy(
            branchId = entry.branchId,
            subjectId = entry.subjectId,
            subjectName = entry.subjectName,
            title = entry.title,
            path = entry.path,
            isPdf = entry.isPdf,
            lastOpenedEpochMs = now(),
        )
    }

    /**
     * Records how far down the article the reader reached. Only ever moves
     * forward: scrolling back up does not un-read what has been read.
     */
    suspend fun recordFurthest(topicId: String, fraction: Float) {
        val existing = _progress.value[topicId] ?: return
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped <= existing.furthest) return
        update(topicId) { it?.copy(furthest = clamped) }
    }

    suspend fun toggleBookmark(topicId: String) =
        update(topicId) { it?.copy(bookmarked = !it.bookmarked) }

    /**
     * Folds another device's reading into this one's and saves the result.
     *
     * Returns the merged map so the caller can push exactly what was stored --
     * reading it back afterwards would race a reader that scrolled in between.
     *
     * The merge rule itself lives in [com.gatemaster.app.core.data.sync.mergeProgress],
     * which is a pure function and is where the decisions are explained.
     */
    suspend fun merge(remote: Map<String, TopicProgress>): Map<String, TopicProgress> {
        load()
        val merged = mutex.withLock {
            val result = com.gatemaster.app.core.data.sync.mergeProgress(_progress.value, remote)
            _progress.value = result
            result
        }
        persist()
        return merged
    }

    private suspend fun update(
        topicId: String,
        transform: (TopicProgress?) -> TopicProgress?,
    ) {
        mutex.withLock {
            val current = _progress.value
            val updated = transform(current[topicId]) ?: return
            _progress.value = current + (topicId to updated)
        }
        persist()
    }

    private suspend fun persist() = withContext(io) {
        runCatching {
            filesDir.mkdirs()
            val tmp = File(file.absolutePath + ".tmp")
            tmp.writeText(json.encodeToString(ProgressFile(topics = _progress.value)))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }.onFailure { Log.e(TAG, "Could not save study progress", it) }
        Unit
    }

    private companion object {
        const val TAG = "StudyProgress"
    }
}

// -- derived views ----------------------------------------------------------

/** Most recently opened topics for a branch, newest first. */
fun Map<String, TopicProgress>.recent(branchId: String, limit: Int = 5): List<TopicProgress> =
    values.filter { it.branchId == branchId && it.lastOpenedEpochMs > 0 }
        .sortedByDescending { it.lastOpenedEpochMs }
        .take(limit)

/** The one to resume: most recent that is not finished. */
fun Map<String, TopicProgress>.continueReading(branchId: String): TopicProgress? =
    values.filter { it.branchId == branchId && !it.isRead && it.lastOpenedEpochMs > 0 }
        .maxByOrNull { it.lastOpenedEpochMs }

fun Map<String, TopicProgress>.bookmarks(branchId: String): List<TopicProgress> =
    values.filter { it.branchId == branchId && it.bookmarked }
        .sortedByDescending { it.lastOpenedEpochMs }

fun Map<String, TopicProgress>.readCount(subjectId: String): Int =
    values.count { it.subjectId == subjectId && it.isRead }

fun Map<String, TopicProgress>.readCountForBranch(branchId: String): Int =
    values.count { it.branchId == branchId && it.isRead }
