package com.gatemaster.app.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One finished sitting.
 *
 * Study progress stays in its JSON file: it is one small document that every
 * screen wants whole. Attempt history is the opposite — it grows without
 * bound and the questions asked of it are aggregates ("accuracy per subject",
 * "which topics cost me marks"), which is a query and not a document.
 */
@Entity(
    tableName = "attempts",
    // Unique, because it is the idempotency key the server files this attempt
    // under: two rows sharing one would mean the same sitting uploaded twice.
    indices = [Index(value = ["clientAttemptId"], unique = true)],
)
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testId: String,
    val title: String,
    val submittedAtEpochMs: Long,
    val score: Double,
    val maxMarks: Int,
    val correct: Int,
    val incorrect: Int,
    val unattempted: Int,
    val timeTakenMs: Long,

    /**
     * Generated on this device when the attempt is recorded, and never
     * changed.
     *
     * The row id cannot do this job: two phones both signed into one account
     * would both call their first attempt 1. This is what makes an upload
     * idempotent, so a retry after a dropped response cannot count the same
     * sitting twice and skew every average built on it.
     *
     * Rows written before sync existed get "local-<id>" in the migration --
     * unique within this device, which is all they need to be, since they have
     * never been anywhere else.
     *
     * The default generates a fresh id rather than leaving it blank. An empty
     * default under a unique index is a trap: the first row takes "" and the
     * second one fails to insert, and the caller that forgot to set it gets a
     * constraint violation rather than the obvious behaviour. The SQL default
     * below stays empty because it exists only for the migration, which fills
     * the column in immediately afterwards.
     */
    @ColumnInfo(defaultValue = "")
    val clientAttemptId: String = java.util.UUID.randomUUID().toString(),

    /** When the server acknowledged this attempt. Null means still to upload. */
    val syncedAt: Long? = null,

    /**
     * The sequence the server filed it under. Null for anything this device
     * has not yet heard back about; the highest of these is the download
     * cursor.
     */
    val serverSeq: Long? = null,
)

/**
 * One question within one sitting.
 *
 * Storing a row per question rather than only the totals is what makes weak
 * topics answerable at all: a per-attempt summary can say a paper went badly
 * but never which topic did.
 */
@Entity(
    tableName = "question_results",
    foreignKeys = [
        ForeignKey(
            entity = AttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["attemptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("attemptId"), Index("subjectId"), Index("topicId")],
)
data class QuestionResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attemptId: Long,
    val questionId: String,
    val subjectId: String?,
    /** The topic id a generated question carried, when it had one. */
    val topicId: String?,
    val marks: Int,
    val marksAwarded: Double,
    /** [com.gatemaster.app.core.model.ResultKind] by name. */
    val kind: String,
)

// -- query results ----------------------------------------------------------

/** How one subject has gone across every attempt. */
data class SubjectAccuracy(
    val subjectId: String,
    val attempted: Int,
    val correct: Int,
    val marksAwarded: Double,
    val marksAvailable: Int,
) {
    val accuracy: Float get() = if (attempted == 0) 0f else correct.toFloat() / attempted
}

/** How one topic has gone. Ordered worst first, so it reads as a to-do list. */
data class TopicAccuracy(
    val topicId: String,
    val subjectId: String?,
    val attempted: Int,
    val correct: Int,
) {
    val accuracy: Float get() = if (attempted == 0) 0f else correct.toFloat() / attempted
}

/** One point on the score trend. */
data class ScorePoint(
    val submittedAtEpochMs: Long,
    val score: Double,
    val maxMarks: Int,
) {
    val percent: Float get() = if (maxMarks == 0) 0f else (score / maxMarks).toFloat()
}

/** The headline totals across every attempt. */
data class ProgressTotals(
    val attempts: Int,
    val correct: Int,
    val incorrect: Int,
    val unattempted: Int,
    val timeSpentMs: Long,
) {
    val attemptedQuestions: Int get() = correct + incorrect

    val accuracy: Float
        get() = if (attemptedQuestions == 0) 0f else correct.toFloat() / attemptedQuestions

    companion object {
        val EMPTY = ProgressTotals(0, 0, 0, 0, 0)
    }
}
