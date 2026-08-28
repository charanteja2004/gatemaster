package com.gatemaster.server.sync

import kotlinx.serialization.Serializable

/**
 * The synced shapes.
 *
 * Two of them, and they are synced by opposite mechanisms, because they are
 * opposite kinds of data:
 *
 * - **Study progress** is mutable shared state. What has been read changes on
 *   whichever device is reading, so two devices genuinely can disagree, and the
 *   protocol has to have an answer for that. It uses optimistic concurrency:
 *   see [ProgressConflict].
 *
 * - **Attempts** are immutable historical facts. A finished paper never changes
 *   afterwards, so there is nothing to disagree about, and the protocol needs
 *   only to be idempotent. It uses append-only upload keyed on a client id.
 *
 * Choosing the mechanism per shape rather than one for both is the difference
 * between a sync that loses reading history and one that does not.
 */

@Serializable
data class ProgressDocument(
    /** The app's own JSON, opaque to the server. */
    val document: String,
    /** Increments on every accepted write. The client sends back what it read. */
    val revision: Long,
)

/**
 * The write was based on a revision the server has already moved past.
 *
 * Carries the current state so the client can merge without a second round
 * trip -- it needs the server's document to merge anyway, and it has just
 * proved it does not have it.
 */
class ProgressConflict(val current: ProgressDocument) :
    Exception("Study progress has changed on another device")

@Serializable
data class SyncedAttempt(
    /** Generated on the device. The idempotency key for upload. */
    val clientAttemptId: String,
    val testId: String,
    val testTitle: String,
    val startedAt: Long,
    val finishedAt: Long,
    val durationSeconds: Int,
    val score: Double,
    val maxScore: Double,
    val questions: List<SyncedAttemptQuestion>,
    /**
     * Assigned by the server on insert; the download cursor. Null on the way
     * up, since the client cannot know it yet.
     */
    val serverSeq: Long? = null,
)

@Serializable
data class SyncedAttemptQuestion(
    val questionId: String,
    val subjectId: String,
    val topicId: String? = null,
    val questionType: String,
    val marks: Double,
    val awarded: Double,
    val wasAttempted: Boolean,
    val wasCorrect: Boolean,
)

/** What an upload did, per attempt, so the client knows what it can stop retrying. */
@Serializable
data class UploadResult(
    val accepted: Int,
    /** Client ids the server already held. Not an error -- the retry worked. */
    val duplicates: List<String>,
    /** The highest sequence the caller now holds, for the next download. */
    val highestSeq: Long,
)

@Serializable
data class AttemptPage(
    val attempts: List<SyncedAttempt>,
    /** Cursor for the next page, or null when the caller is up to date. */
    val nextSince: Long?,
)
