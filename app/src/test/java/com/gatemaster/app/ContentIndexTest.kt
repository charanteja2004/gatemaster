package com.gatemaster.app

import com.gatemaster.app.core.model.ContentIndex
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * Validates the content index that actually ships in the APK.
 *
 * This is the check the project did not have: the previous generator indexed
 * only *.html, so "Previous Papers" shipped with zero topics and 22 MB of PDFs
 * were unreachable — and nothing failed. Every assertion here is about the real
 * assets on disk, so content drift breaks the build instead of the app.
 */
class ContentIndexTest {

    @Test
    fun `index parses`() {
        assertTrue("schemaVersion should be set", index.schemaVersion >= 2)
        assertTrue("expected subjects", index.subjects.isNotEmpty())
        assertTrue("expected previous-year papers", index.papers.isNotEmpty())
    }

    @Test
    fun `every referenced asset exists`() {
        val missing = mutableListOf<String>()

        fun check(path: String, owner: String) {
            if (!File(assetsDir, path).isFile) missing += "$owner -> $path"
        }

        for (subject in index.subjects) {
            subject.topics.forEach { check(it.content.path, "${subject.id}/${it.id}") }
            subject.referenceNotes.forEach { check(it.content.path, "${subject.id}/${it.id}") }
            subject.shortNotes?.let { check(it.path, "${subject.id}/shortNotes") }
        }
        for (paper in index.papers) {
            check(paper.paper.path, paper.id)
            paper.answerKey?.let { check(it.path, "${paper.id}/key") }
        }

        assertTrue("Assets referenced by the index but missing on disk: $missing", missing.isEmpty())
    }

    @Test
    fun `ids are unique`() {
        val ids = index.subjects.flatMap { subject ->
            subject.topics.map { it.id } + subject.referenceNotes.map { it.id }
        } + index.papers.map { it.id }

        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertTrue("Duplicate ids: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `titles are presentable`() {
        val bad = mutableListOf<String>()
        for (subject in index.subjects) {
            for (topic in subject.topics) {
                val t = topic.title
                when {
                    t.isBlank() -> bad += "${topic.id}: blank"
                    // The VS Code boilerplate title leaked into most files.
                    t.equals("Document", ignoreCase = true) -> bad += "${topic.id}: boilerplate"
                    t.length > 52 -> bad += "${topic.id}: ${t.length} chars"
                    t.any { it.isLowerCase() }.not() && t.length > 4 ->
                        bad += "${topic.id}: shouting"
                }
            }
        }
        assertTrue("Topic titles needing attention: $bad", bad.isEmpty())
    }

    @Test
    fun `every html article is reachable from the index`() {
        val indexed = buildSet {
            for (subject in index.subjects) {
                subject.topics.forEach { add(it.content.path) }
                subject.referenceNotes.forEach { add(it.content.path) }
                subject.shortNotes?.let { add(it.path) }
            }
        }

        // shortnotes are indexed per subject; testseries is legacy scratch data.
        val excludedDirs = setOf("shortnotes", "testseries")

        val orphans = assetsDir.listFiles { f -> f.isDirectory }
            .orEmpty()
            .filter { it.name !in excludedDirs }
            .flatMap { dir ->
                dir.listFiles { f -> f.extension.equals("html", ignoreCase = true) }
                    .orEmpty()
                    .map { "${dir.name}/${it.name}" }
            }
            .filterNot { it in indexed }

        assertTrue(
            "HTML articles present in assets but not reachable in the app: $orphans",
            orphans.isEmpty(),
        )
    }

    @Test
    fun `papers are newest first and years are distinct`() {
        val years = index.papers.map { it.year }
        assertEquals("Papers should be sorted newest first", years.sortedDescending(), years)
        assertEquals("Duplicate paper years", years.distinct().size, years.size)
    }

    @Test
    fun `engineering mathematics has content`() {
        // Regression guard: the old app advertised Mathematics on the home
        // screen and silently did nothing when tapped, even though six maths
        // PDFs were sitting unused in assets.
        val maths = index.subjects.single { it.id == "maths" }
        assertTrue("Mathematics should not be empty", !maths.isEmpty)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

        private lateinit var assetsDir: File
        private lateinit var index: ContentIndex

        @BeforeClass
        @JvmStatic
        fun loadIndex() {
            assetsDir = findAssetsDir()
            val file = File(assetsDir, "content_index.json")
            assertTrue("content_index.json not found at ${file.absolutePath}", file.isFile)
            index = json.decodeFromString(file.readText())
        }

        /** Unit tests run from the module directory, but do not rely on it. */
        private fun findAssetsDir(): File {
            var dir: File? = File("").absoluteFile
            repeat(4) {
                val candidate = File(dir, "src/main/assets")
                if (candidate.isDirectory) return candidate
                val nested = File(dir, "app/src/main/assets")
                if (nested.isDirectory) return nested
                dir = dir?.parentFile
            }
            error("Could not locate app/src/main/assets from ${File("").absolutePath}")
        }
    }
}
