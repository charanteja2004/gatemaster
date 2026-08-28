package com.gatemaster.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes.
 *
 * Only identifiers and short strings cross a route boundary. Nothing here is a
 * serialized object graph — the old app put a whole `Topic` into an Intent
 * extra via java.io.Serializable, which is slow and breaks the moment a field
 * is added.
 */

@Serializable
data object HomeRoute

@Serializable
data class SubjectRoute(val subjectId: String)

@Serializable
data object PapersRoute

@Serializable
data object SearchRoute

/** Bottom-bar tab: the full subject list for the selected paper. */
@Serializable
data object SubjectsRoute

/** Bottom-bar tab: theme, paper and about. */
@Serializable
data object SettingsRoute

/** Sign in, sign out, and which sync server this install talks to. */
@Serializable
data object AccountRoute

/** [firstRun] hides the back button: there is nothing behind it yet. */
@Serializable
data class BranchPickerRoute(val firstRun: Boolean = false)

/** Bottom-bar tab: what the attempt history says about how it is going. */
@Serializable
data object ProgressRoute

@Serializable
data object TestListRoute

/** [restart] discards any saved attempt and begins the test from scratch. */
@Serializable
data class TestPlayerRoute(val testId: String, val restart: Boolean = false)

/**
 * The document viewer. [path] is relative to the assets root; [isPdf] selects
 * the renderer.
 */
@Serializable
data class ReaderRoute(
    val title: String,
    val subtitle: String,
    val path: String,
    val isPdf: Boolean,
    /** Set when the document is a topic, so the reader can offer prev/next. */
    val subjectId: String? = null,
    val topicId: String? = null,
)
