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

/** [firstRun] hides the back button: there is nothing behind it yet. */
@Serializable
data class BranchPickerRoute(val firstRun: Boolean = false)

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
)
