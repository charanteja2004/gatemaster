package com.gatemaster.app

import com.gatemaster.app.core.data.StudyProgressRepository
import com.gatemaster.app.core.data.TopicProgress
import com.gatemaster.app.core.data.bookmarks
import com.gatemaster.app.core.data.continueReading
import com.gatemaster.app.core.data.readCount
import com.gatemaster.app.core.data.readCountForBranch
import com.gatemaster.app.core.data.recent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Reading progress: the two rules that are easy to state and easy to break —
 * furthest-read only moves forward, and the last tenth counts as read.
 *
 * The clock is injected so ordering is exact rather than dependent on how fast
 * the test machine runs.
 */
class StudyProgressRepositoryTest {

    @get:Rule
    val temp = TemporaryFolder()

    private var clock = 1_000L

    private fun repository(filesDir: File = temp.newFolder()) =
        StudyProgressRepository(filesDir, now = { clock })

    private fun topic(
        id: String,
        subjectId: String = "algo",
        branchId: String = "cs",
    ) = TopicProgress(
        topicId = id,
        branchId = branchId,
        subjectId = subjectId,
        subjectName = "Algorithms",
        title = "Topic $id",
        path = "algo/$id.html",
    )

    // -- the two rules --------------------------------------------------------

    @Test
    fun `furthest read only ever moves forward`() = runTest {
        val repo = repository()
        repo.load()
        repo.recordOpen(topic("t1"))

        repo.recordFurthest("t1", 0.6f)
        repo.recordFurthest("t1", 0.2f)

        assertEquals(0.6f, repo.progress.value.getValue("t1").furthest, 1e-6f)
    }

    @Test
    fun `reaching the last tenth counts as read`() = runTest {
        val repo = repository()
        repo.load()
        repo.recordOpen(topic("t1"))

        repo.recordFurthest("t1", 0.89f)
        assertFalse(repo.progress.value.getValue("t1").isRead)

        repo.recordFurthest("t1", 0.9f)
        assertTrue(repo.progress.value.getValue("t1").isRead)
    }

    @Test
    fun `scroll fractions outside the page are clamped`() = runTest {
        val repo = repository()
        repo.load()
        repo.recordOpen(topic("t1"))

        repo.recordFurthest("t1", 4f)

        assertEquals(1f, repo.progress.value.getValue("t1").furthest, 1e-6f)
    }

    @Test
    fun `progress for a topic that was never opened is ignored`() = runTest {
        val repo = repository()
        repo.load()

        repo.recordFurthest("never-opened", 0.5f)

        assertTrue(
            "recording progress should not conjure a topic into the list",
            repo.progress.value.isEmpty(),
        )
    }

    // -- reopening ------------------------------------------------------------

    @Test
    fun `reopening a topic keeps how far it was read and its bookmark`() = runTest {
        val repo = repository()
        repo.load()
        repo.recordOpen(topic("t1"))
        repo.recordFurthest("t1", 0.95f)
        repo.toggleBookmark("t1")

        clock = 2_000
        repo.recordOpen(topic("t1"))

        val stored = repo.progress.value.getValue("t1")
        assertEquals(0.95f, stored.furthest, 1e-6f)
        assertTrue(stored.bookmarked)
        assertEquals(2_000L, stored.lastOpenedEpochMs)
    }

    @Test
    fun `reopening picks up a renamed topic`() = runTest {
        // Titles come from the generated index, so re-running the content
        // pipeline can legitimately rename one.
        val repo = repository()
        repo.load()
        repo.recordOpen(topic("t1"))

        repo.recordOpen(topic("t1").copy(title = "A better title"))

        assertEquals("A better title", repo.progress.value.getValue("t1").title)
    }

    @Test
    fun `bookmarks toggle both ways`() = runTest {
        val repo = repository()
        repo.load()
        repo.recordOpen(topic("t1"))

        repo.toggleBookmark("t1")
        assertTrue(repo.progress.value.getValue("t1").bookmarked)

        repo.toggleBookmark("t1")
        assertFalse(repo.progress.value.getValue("t1").bookmarked)
    }

    // -- persistence ----------------------------------------------------------

    @Test
    fun `progress survives a restart`() = runTest {
        val filesDir = temp.newFolder()
        repository(filesDir).apply {
            load()
            recordOpen(topic("t1"))
            recordFurthest("t1", 0.5f)
            toggleBookmark("t1")
        }

        val reopened = repository(filesDir)
        reopened.load()

        val stored = reopened.progress.value.getValue("t1")
        assertEquals(0.5f, stored.furthest, 1e-6f)
        assertTrue(stored.bookmarked)
    }

    @Test
    fun `an unreadable progress file is discarded rather than failing the launch`() = runTest {
        val filesDir = temp.newFolder()
        repository(filesDir).apply {
            load()
            recordOpen(topic("t1"))
        }
        File(filesDir, "study_progress.json").writeText("{ truncated")

        val reopened = repository(filesDir)
        reopened.load()

        assertTrue(reopened.progress.value.isEmpty())
        assertFalse(
            "the corrupt file should be cleared, not read again next launch",
            File(filesDir, "study_progress.json").exists(),
        )
    }

    @Test
    fun `a fresh install loads an empty history`() = runTest {
        val repo = repository()
        repo.load()

        assertTrue(repo.progress.value.isEmpty())
    }

    // -- derived views --------------------------------------------------------

    @Test
    fun `recent lists newest first and stays within its branch`() = runTest {
        val repo = repository()
        repo.load()
        repo.recordOpen(topic("t1"))
        clock = 2_000
        repo.recordOpen(topic("t2"))
        clock = 3_000
        repo.recordOpen(topic("me1", subjectId = "thermo", branchId = "me"))

        val recent = repo.progress.value.recent("cs")

        assertEquals(listOf("t2", "t1"), recent.map { it.topicId })
    }

    @Test
    fun `continue reading skips what has been finished`() = runTest {
        val repo = repository()
        repo.load()
        repo.recordOpen(topic("t1"))
        clock = 2_000
        repo.recordOpen(topic("t2"))
        repo.recordFurthest("t2", 1f)

        // t2 is the most recent, but it is done — the point is to resume.
        assertEquals("t1", repo.progress.value.continueReading("cs")?.topicId)
    }

    @Test
    fun `continue reading is empty once everything is read`() = runTest {
        val repo = repository()
        repo.load()
        repo.recordOpen(topic("t1"))
        repo.recordFurthest("t1", 1f)

        assertNull(repo.progress.value.continueReading("cs"))
    }

    @Test
    fun `read counts are per subject and per branch`() = runTest {
        val repo = repository()
        repo.load()
        repo.recordOpen(topic("t1"))
        repo.recordFurthest("t1", 1f)
        repo.recordOpen(topic("t2"))
        repo.recordFurthest("t2", 0.3f)
        repo.recordOpen(topic("d1", subjectId = "dbms"))
        repo.recordFurthest("d1", 1f)

        val progress = repo.progress.value
        assertEquals(1, progress.readCount("algo"))
        assertEquals(1, progress.readCount("dbms"))
        assertEquals(2, progress.readCountForBranch("cs"))
    }

    @Test
    fun `bookmarks are listed per branch`() = runTest {
        val repo = repository()
        repo.load()
        repo.recordOpen(topic("t1"))
        repo.toggleBookmark("t1")
        repo.recordOpen(topic("me1", subjectId = "thermo", branchId = "me"))
        repo.toggleBookmark("me1")

        assertEquals(listOf("t1"), repo.progress.value.bookmarks("cs").map { it.topicId })
        assertEquals(listOf("me1"), repo.progress.value.bookmarks("me").map { it.topicId })
    }
}
