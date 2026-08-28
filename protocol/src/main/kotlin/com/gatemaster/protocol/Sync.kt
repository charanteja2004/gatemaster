package com.gatemaster.protocol

import kotlinx.serialization.Serializable

/**
 * The sync half of the wire contract.
 *
 * Two synced shapes, synced by opposite mechanisms, because they are opposite
 * kinds of data:
 *
 * - **Study progress** is mutable shared state. What has been read changes on
 *   whichever device is reading, so two devices genuinely can disagree.
 *   Optimistic concurrency on [ProgressResponse.revision] is the answer.
 * - **Attempts** are immutable historical facts. A finished paper never changes
 *   afterwards, so there is nothing to disagree about and the protocol needs
 *   only to be idempotent -- which [SyncedAttempt.clientAttemptId] provides.
 */

@Serializable
data class ProgressResponse(
    /** The app's own JSON, opaque to the server. */
    val document: String,
    /** Increments on every accepted write. Zero means never synced. */
    val revision: Long,
)

@Serializable
data class ProgressPutRequest(val document: String, val revision: Long)

/**
 * A rejected progress write, carrying the server's current document.
 *
 * The client needs the server's copy to merge, and it has just proved it does
 * not have it, so sending it with the rejection saves a round trip that would
 * otherwise happen every single time.
 */
@Serializable
data class ProgressConflictResponse(
    val code: String = "progress_conflict",
    val message: String = "",
    val current: ProgressResponse,
)

@Serializable
data class SyncedAttempt(
    /**
     * Generated on the device, stable across retries. The idempotency key: it
     * is what stops a retry after a dropped response counting the attempt
     * twice and skewing every average computed from it.
     */
    val clientAttemptId: String,
    val testId: String,
    val testTitle: String,
    val startedAt: Long,
    val finishedAt: Long,
    val durationSeconds: Int,
    val score: Double,
    val maxScore: Double,
    val questions: List<SyncedAttemptQuestion> = emptyList(),
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
    /**
     * MCQ, MSQ or NAT, when the client knows. It often does not: an attempt
     * row records whether the answer was right, not what shape the question
     * was, so this is a nice-to-have and never a requirement.
     */
    val questionType: String? = null,
    val marks: Double,
    val awarded: Double,
    val wasAttempted: Boolean,
    val wasCorrect: Boolean,
)

@Serializable
data class UploadAttemptsRequest(val attempts: List<SyncedAttempt>)

/** What an upload did, per attempt, so the client knows what to stop retrying. */
@Serializable
data class UploadResult(
    val accepted: Int,
    /** Client ids the server already held. Not an error -- the retry worked. */
    val duplicates: List<String> = emptyList(),
    /** The highest sequence the caller now holds, for the next download. */
    val highestSeq: Long = 0,
)

@Serializable
data class AttemptPage(
    val attempts: List<SyncedAttempt> = emptyList(),
    /** Cursor for the next page, or null when the caller is up to date. */
    val nextSince: Long? = null,
)
