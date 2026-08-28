package com.gatemaster.app.core.data.sync

import android.util.Log
import com.gatemaster.app.core.data.StudyProgressRepository
import com.gatemaster.app.core.data.TopicProgress
import com.gatemaster.app.core.data.auth.ApiError
import com.gatemaster.app.core.data.auth.ApiResult
import com.gatemaster.app.core.data.auth.SessionStore
import com.gatemaster.app.core.data.auth.SyncApi
import com.gatemaster.app.core.data.db.AttemptDao
import com.gatemaster.app.core.data.db.AttemptEntity
import com.gatemaster.app.core.data.db.QuestionResultEntity
import com.gatemaster.protocol.SyncedAttempt
import com.gatemaster.protocol.SyncedAttemptQuestion
import kotlinx.serialization.json.Json

/** How a sync ended. */
sealed interface SyncOutcome {
    data class Success(
        val attemptsUploaded: Int,
        val attemptsDownloaded: Int,
        val progressPushed: Boolean,
        val progressPulled: Boolean,
    ) : SyncOutcome

    /** Nobody is signed in, or no server is set. Not a failure; nothing to do. */
    data object NothingToDo : SyncOutcome

    /** Try again later: no network, or the server is down. */
    data class Retry(val reason: String) : SyncOutcome

    /** The session is gone. Retrying will not help until the user signs in. */
    data object SignedOut : SyncOutcome
}

/**
 * One sync cycle: reading history up and down, attempts up and down.
 *
 * The whole thing is written to be safe to run at any moment and safe to
 * interrupt at any moment, because on a phone it will be. Nothing here assumes
 * it finished last time.
 */
class SyncManager(
    private val api: SyncApi,
    private val studyProgress: StudyProgressRepository,
    private val dao: AttemptDao,
    private val tokens: SessionStore,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun sync(): SyncOutcome {
        if (tokens.current() == null) return SyncOutcome.NothingToDo

        val attempts = syncAttempts()
        if (attempts is SyncOutcome.Retry || attempts is SyncOutcome.SignedOut) return attempts

        val progress = syncProgress()
        if (progress is SyncOutcome.Retry || progress is SyncOutcome.SignedOut) return progress

        val a = attempts as? SyncOutcome.Success
        val p = progress as? SyncOutcome.Success
        return SyncOutcome.Success(
            attemptsUploaded = a?.attemptsUploaded ?: 0,
            attemptsDownloaded = a?.attemptsDownloaded ?: 0,
            progressPushed = p?.progressPushed ?: false,
            progressPulled = p?.progressPulled ?: false,
        )
    }

    // --- Study progress -----------------------------------------------------

    /**
     * Pulls the server's document, merges, and pushes the result.
     *
     * The merge always runs, even when nothing local changed: the pull is what
     * brings the other device's reading over, and merging an unchanged local
     * copy with it is how that lands.
     *
     * A conflict means another device wrote between this pull and this push.
     * That is answered by merging again against what the rejection carried and
     * pushing once more -- not by looping, because a second conflict means
     * something is writing continuously and the next scheduled sync will do
     * just as well as a tight retry.
     */
    private suspend fun syncProgress(): SyncOutcome {
        studyProgress.load()

        val remote = when (val result = api.progress()) {
            is ApiResult.Ok -> result.value
            is ApiResult.Failed -> return result.error.toOutcome()
        }

        val merged = studyProgress.merge(decode(remote.document))
        val encoded = json.encodeToString(merged)

        // Nothing to push when the server already holds exactly this. Skipping
        // saves a write and, more usefully, stops two idle devices bouncing
        // revisions off each other forever.
        if (encoded == remote.document) {
            return SyncOutcome.Success(0, 0, progressPushed = false, progressPulled = true)
        }

        return when (val push = api.putProgress(encoded, remote.revision)) {
            is ApiResult.Ok -> SyncOutcome.Success(0, 0, progressPushed = true, progressPulled = true)

            is ApiResult.Failed -> when (val error = push.error) {
                is ApiError.Conflict -> retryProgressOnce(error)
                else -> error.toOutcome()
            }
        }
    }

    private suspend fun retryProgressOnce(conflict: ApiError.Conflict): SyncOutcome {
        val merged = studyProgress.merge(decode(conflict.current.document))
        val encoded = json.encodeToString(merged)

        return when (val push = api.putProgress(encoded, conflict.current.revision)) {
            is ApiResult.Ok ->
                SyncOutcome.Success(0, 0, progressPushed = true, progressPulled = true)

            is ApiResult.Failed -> {
                Log.i(TAG, "Progress still conflicting after one merge; leaving it for next sync")
                // Not an error the user should see. The merge is saved locally
                // either way, so nothing was lost -- only postponed.
                SyncOutcome.Success(0, 0, progressPushed = false, progressPulled = true)
            }
        }
    }

    private fun decode(document: String): Map<String, TopicProgress> {
        if (document.isBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, TopicProgress>>(document) }
            .getOrElse {
                // A document this build cannot read is not a reason to wipe it
                // or to fail: treat the server as empty, merge, and the next
                // push replaces it with something readable.
                Log.w(TAG, "Ignoring unreadable remote progress document", it)
                emptyMap()
            }
    }

    // --- Attempts -----------------------------------------------------------

    private suspend fun syncAttempts(): SyncOutcome {
        val uploaded = uploadAttempts()
        if (uploaded is SyncOutcome.Retry || uploaded is SyncOutcome.SignedOut) return uploaded

        val downloaded = downloadAttempts()
        if (downloaded is SyncOutcome.Retry || downloaded is SyncOutcome.SignedOut) return downloaded

        return SyncOutcome.Success(
            attemptsUploaded = (uploaded as? SyncOutcome.Success)?.attemptsUploaded ?: 0,
            attemptsDownloaded = (downloaded as? SyncOutcome.Success)?.attemptsDownloaded ?: 0,
            progressPushed = false,
            progressPulled = false,
        )
    }

    private suspend fun uploadAttempts(): SyncOutcome {
        val pending = dao.unsyncedAttempts(UPLOAD_BATCH)
        if (pending.isEmpty()) return SyncOutcome.Success(0, 0, false, false)

        val results = dao.resultsFor(pending.map { it.id }).groupBy { it.attemptId }
        val payload = pending.map { attempt ->
            attempt.toSynced(results[attempt.id].orEmpty())
        }

        return when (val result = api.uploadAttempts(payload)) {
            is ApiResult.Ok -> {
                // Duplicates are marked synced too. The server already has
                // them, so leaving them pending would retry the same rejected
                // rows on every sync, for ever.
                dao.markSynced(payload.map { it.clientAttemptId }, now())
                SyncOutcome.Success(result.value.accepted, 0, false, false)
            }

            is ApiResult.Failed -> result.error.toOutcome()
        }
    }

    private suspend fun downloadAttempts(): SyncOutcome {
        var since = dao.highestServerSeq()
        var stored = 0
        var pages = 0

        while (pages < MAX_PAGES) {
            val page = when (val result = api.attemptsSince(since, DOWNLOAD_PAGE)) {
                is ApiResult.Ok -> result.value
                is ApiResult.Failed -> return result.error.toOutcome()
            }

            // Filter before inserting rather than relying on the unique index
            // alone: the rows this device uploaded come back down on the next
            // page, and inserting them would be a wasted transaction each.
            val known = dao.existingClientIds(page.attempts.map { it.clientAttemptId }).toSet()
            for (attempt in page.attempts) {
                if (attempt.clientAttemptId in known) {
                    // Already here. Still has to record where the server filed
                    // it, or the cursor never moves past the rows this device
                    // uploaded and this page comes down again every sync.
                    attempt.serverSeq?.let { seq ->
                        dao.recordServerSeq(attempt.clientAttemptId, seq, now())
                    }
                    continue
                }
                dao.storeDownloaded(attempt.toEntity(now()), attempt.toResults())
                stored++
            }

            since = page.attempts.lastOrNull()?.serverSeq ?: since
            pages++
            if (page.nextSince == null) break
        }

        return SyncOutcome.Success(0, stored, false, false)
    }

    private fun ApiError.toOutcome(): SyncOutcome = when (this) {
        ApiError.NotConfigured -> SyncOutcome.NothingToDo
        ApiError.SignedOut -> SyncOutcome.SignedOut
        is ApiError.Unreachable -> SyncOutcome.Retry(cause.message ?: "unreachable")
        is ApiError.Rejected -> SyncOutcome.Retry("server said ${body.code}")
        is ApiError.Conflict -> SyncOutcome.Retry("conflict")
    }

    private companion object {
        const val TAG = "SyncManager"
        const val UPLOAD_BATCH = 100
        const val DOWNLOAD_PAGE = 50

        /**
         * A stop on the download loop.
         *
         * Without it a server that always advertises another page -- a bug, or
         * a cursor that fails to advance -- would keep a background worker
         * running until the platform killed it. Whatever is left comes down on
         * the next sync.
         */
        const val MAX_PAGES = 20
    }
}

// --- Mapping ---------------------------------------------------------------

private fun AttemptEntity.toSynced(results: List<QuestionResultEntity>) = SyncedAttempt(
    clientAttemptId = clientAttemptId,
    testId = testId,
    testTitle = title,
    // The device never recorded a start time separately, so it is derived:
    // submitted, less however long the sitting took.
    startedAt = submittedAtEpochMs - timeTakenMs,
    finishedAt = submittedAtEpochMs,
    durationSeconds = (timeTakenMs / 1000).toInt(),
    score = score,
    maxScore = maxMarks.toDouble(),
    questions = results.map { result ->
        SyncedAttemptQuestion(
            questionId = result.questionId,
            subjectId = result.subjectId.orEmpty(),
            topicId = result.topicId,
            // Deliberately not sent: `kind` is CORRECT / INCORRECT /
            // UNATTEMPTED, which is the outcome, not the question type. The
            // two booleans below already carry the outcome, and inventing a
            // type here would put a wrong value in the server's column.
            questionType = null,
            marks = result.marks.toDouble(),
            awarded = result.marksAwarded,
            wasAttempted = result.kind != "UNATTEMPTED",
            wasCorrect = result.kind == "CORRECT",
        )
    },
)

private fun SyncedAttempt.toEntity(syncedAt: Long) = AttemptEntity(
    testId = testId,
    title = testTitle,
    submittedAtEpochMs = finishedAt,
    score = score,
    maxMarks = maxScore.toInt(),
    correct = questions.count { it.wasCorrect },
    incorrect = questions.count { it.wasAttempted && !it.wasCorrect },
    unattempted = questions.count { !it.wasAttempted },
    timeTakenMs = durationSeconds * 1000L,
    clientAttemptId = clientAttemptId,
    // It came from the server, so it is by definition already there.
    syncedAt = syncedAt,
    serverSeq = serverSeq,
)

private fun SyncedAttempt.toResults() = questions.map { question ->
    QuestionResultEntity(
        attemptId = 0,
        questionId = question.questionId,
        subjectId = question.subjectId.ifBlank { null },
        topicId = question.topicId,
        marks = question.marks.toInt(),
        marksAwarded = question.awarded,
        kind = when {
            !question.wasAttempted -> "UNATTEMPTED"
            question.wasCorrect -> "CORRECT"
            else -> "INCORRECT"
        },
    )
}
