package com.gatemaster.app

import com.gatemaster.app.core.model.BranchDetail
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
        assertTrue("schemaVersion should be 3 or later", index.schemaVersion >= 3)
        assertTrue("expected branches", index.branches.isNotEmpty())
        assertTrue("expected previous-year papers", index.papers.isNotEmpty())
    }

    @Test
    fun `all thirty GATE papers are present`() {
        assertEquals(30, index.branches.size)
        // A representative spread of paper codes, including the three added for 2026.
        listOf("CS", "ME", "EE", "EC", "CE", "DA", "GE", "NM", "XE", "XH").forEach { code ->
            assertTrue(
                "missing paper code $code",
                index.branches.any { it.code == code },
            )
        }
    }

    @Test
    fun `paper codes and branch ids are unique`() {
        val codes = index.branches.map { it.code }
        assertEquals("duplicate paper codes", codes.distinct().size, codes.size)
        val ids = index.branches.map { it.id }
        assertEquals("duplicate branch ids", ids.distinct().size, ids.size)
    }

    @Test
    fun `every branch has General Aptitude, which is 15 marks in every paper`() {
        index.branches.forEach { branch ->
            val ga = branch.subjects.firstOrNull { it.id == "aptitude" }
            assertTrue("${branch.code} has no General Aptitude", ga != null)
            assertEquals("${branch.code} GA weightage", 15, ga!!.weightage)
            assertTrue("${branch.code} GA has no notes", ga.topics.isNotEmpty())
        }
    }

    @Test
    fun `every branch has named, weighted subjects`() {
        index.branches.forEach { branch ->
            assertTrue("${branch.code} has no subjects", branch.subjects.isNotEmpty())
            branch.subjects.forEach { subject ->
                assertTrue("${branch.code}/${subject.id} unnamed", subject.name.isNotBlank())
                assertTrue(
                    "${branch.code}/${subject.id} has no weightage",
                    subject.weightage > 0,
                )
            }
        }
    }

    @Test
    fun `a subject without a syllabus only ever appears in an outline paper`() {
        // Outline papers list their sections but not yet the detailed syllabus.
        // The UI explains that; what must never happen is a *detailed* paper
        // shipping a subject with nothing behind it.
        index.branches.forEach { branch ->
            branch.subjects.filter { it.isEmpty }.forEach { subject ->
                assertEquals(
                    "${branch.code}/${subject.id} is empty but the paper is marked detailed",
                    BranchDetail.OUTLINE,
                    branch.detail,
                )
            }
        }
    }

    @Test
    fun `detailed branches carry a syllabus for every subject`() {
        index.branches.filter { it.detail == BranchDetail.FULL }.forEach { branch ->
            branch.subjects.forEach { subject ->
                assertTrue(
                    "${branch.code}/${subject.id} is marked detailed but has no syllabus",
                    subject.syllabus.isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun `every referenced asset exists`() {
        val missing = mutableListOf<String>()

        fun check(path: String, owner: String) {
            val file = File(assetsDir, path)
            if (file.isFile) return
            // PDFs are not kept in the repository, so a checkout without them
            // is expected rather than broken. A missing PDF from a folder that
            // *is* present is still real drift, and still fails.
            if (path.endsWith(".pdf", ignoreCase = true) && !file.parentFile.isDirectory) return
            missing += "$owner -> $path"
        }

        for (branch in index.branches) {
            for (subject in branch.subjects) {
                subject.topics.forEach { check(it.content.path, "${branch.code}/${it.id}") }
                subject.referenceNotes.forEach {
                    check(it.content.path, "${branch.code}/${it.id}")
                }
                subject.shortNotes?.let { check(it.path, "${branch.code}/${subject.id}/short") }
            }
        }
        for (paper in index.papers) {
            check(paper.paper.path, paper.id)
            paper.answerKey?.let { check(it.path, "${paper.id}/key") }
        }

        assertTrue("Assets referenced by the index but missing on disk: $missing", missing.isEmpty())
    }

    @Test
    fun `topic ids are unique within a branch`() {
        index.branches.forEach { branch ->
            val ids = branch.subjects.flatMap { subject ->
                subject.topics.map { it.id } + subject.referenceNotes.map { it.id }
            }
            val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            assertTrue("${branch.code} has duplicate ids: $duplicates", duplicates.isEmpty())
        }
    }

    @Test
    fun `titles are presentable`() {
        val bad = mutableListOf<String>()
        for (subject in csBranch.subjects) {
            for (topic in subject.topics) {
                val t = topic.title
                when {
                    t.isBlank() -> bad += "${topic.id}: blank"
                    // The VS Code boilerplate title leaked into most files.
                    t.equals("Document", ignoreCase = true) -> bad += "${topic.id}: boilerplate"
                    t.length > 52 -> bad += "${topic.id}: ${t.length} chars"
                    t.none { it.isLowerCase() } && t.length > 4 -> bad += "${topic.id}: shouting"
                    t.none { it.isUpperCase() } -> bad += "${topic.id}: no capital"
                }
            }
        }
        assertTrue("Topic titles needing attention: $bad", bad.isEmpty())
    }

    @Test
    fun `every html article is reachable from the index`() {
        val indexed = buildSet {
            for (branch in index.branches) {
                for (subject in branch.subjects) {
                    subject.topics.forEach { add(it.content.path) }
                    subject.referenceNotes.forEach { add(it.content.path) }
                    subject.shortNotes?.let { add(it.path) }
                }
            }
        }

        // shortnotes are indexed per subject rather than as their own folder.
        val excludedDirs = setOf("shortnotes")

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
    fun `engineering mathematics has content in CS`() {
        // Regression guard: the old app advertised Mathematics on the home
        // screen and silently did nothing when tapped, even though six maths
        // PDFs were sitting unused in assets.
        val maths = csBranch.subjects.single { it.id == "maths" }
        assertTrue("Mathematics should not be syllabus-only in CS", !maths.isSyllabusOnly)
    }

    @Test
    fun `topics added to close known gaps are present`() {
        // These were reachable only as broken links before; the audit surfaced
        // them as genuine content gaps rather than typos.
        val csTopicPaths = csBranch.subjects.flatMap { it.topics }.map { it.content.path }
        listOf(
            "ds/bfs.html",
            "ds/dfs.html",
            "ds/dll.html",
            "ds/variables.html",
            "algo/spacecomplexity.html",
            "algo/strassen.html",
        ).forEach { path ->
            assertTrue("missing newly written article $path", path in csTopicPaths)
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

        private lateinit var assetsDir: File
        private lateinit var index: ContentIndex

        private val csBranch get() = index.branch("cs")!!

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
