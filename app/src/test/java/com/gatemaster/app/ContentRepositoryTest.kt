package com.gatemaster.app

import com.gatemaster.app.core.data.ContentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reading the content index, and the search ranking on top of it.
 *
 * [ContentIndexTest] checks the index that really ships; this checks the
 * repository's behaviour against a fixture, including the failure paths that
 * real assets deliberately never exercise.
 */
class ContentRepositoryTest {

    private fun topic(id: String, title: String, order: Int) =
        """
        {
          "id": "$id",
          "title": "$title",
          "order": $order,
          "content": {"type": "html", "path": "algo/$id.html"}
        }
        """.trimIndent()

    private val index = """
        {
          "schemaVersion": 3,
          "branches": [
            {
              "id": "cs",
              "code": "CS",
              "name": "Computer Science and Information Technology",
              "shortName": "CS",
              "order": 1,
              "detail": "full",
              "hasNotes": true,
              "paperIds": ["cs-2024", "cs-2023"],
              "subjects": [
                {
                  "id": "algo",
                  "name": "Algorithms",
                  "shortName": "Algo",
                  "weightage": 8,
                  "order": 2,
                  "syllabus": ["Searching, sorting, hashing"],
                  "topics": [
                    ${topic("t1", "Binary Search Tree", 1)},
                    ${topic("t2", "Complete Binary Tree", 2)},
                    ${topic("t3", "Rebinning Histograms", 3)},
                    ${topic("t4", "Dijkstra", 4)}
                  ]
                },
                {
                  "id": "aptitude",
                  "name": "General Aptitude",
                  "shortName": "GA",
                  "weightage": 15,
                  "order": 1,
                  "syllabus": ["Verbal aptitude"]
                }
              ]
            },
            {
              "id": "me",
              "code": "ME",
              "name": "Mechanical Engineering",
              "shortName": "ME",
              "order": 2,
              "detail": "outline"
            }
          ],
          "papers": [
            {
              "id": "cs-2023",
              "year": 2023,
              "title": "GATE CS 2023",
              "paper": {"type": "pdf", "path": "previousPapers/cs2023.pdf"}
            },
            {
              "id": "cs-2024",
              "year": 2024,
              "title": "GATE CS 2024",
              "paper": {"type": "pdf", "path": "previousPapers/cs2024.pdf"},
              "answerKey": {"type": "pdf", "path": "previousPapers/cs2024key.pdf"}
            },
            {
              "id": "ee-2024",
              "year": 2024,
              "title": "GATE EE 2024",
              "paper": {"type": "pdf", "path": "previousPapers/ee2024.pdf"}
            }
          ]
        }
    """.trimIndent()

    /** Every asset the fixture index names, as a build that carries them all. */
    private val bundled = listOf(
        "algo/t1.html", "algo/t2.html", "algo/t3.html", "algo/t4.html",
        "previousPapers/cs2023.pdf", "previousPapers/cs2024.pdf",
        "previousPapers/cs2024key.pdf", "previousPapers/ee2024.pdf",
    ).associateWith { "<html></html>" }

    private fun repository(
        json: String = index,
        assets: Map<String, String> = bundled,
    ) = ContentRepository(FakeAssetSource(assets + ("content_index.json" to json)))

    // -- loading --------------------------------------------------------------

    @Test
    fun `the index parses`() = runTest {
        val result = repository().index()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().branches.size)
    }

    @Test
    fun `a malformed index surfaces as a failure rather than an empty app`() = runTest {
        // The point of Result here: the old version logged the exception and
        // showed an empty course list, which looked like "no content" instead
        // of "something is broken".
        assertTrue(repository("{ not json").index().isFailure)
    }

    @Test
    fun `a missing index surfaces as a failure`() = runTest {
        val repo = ContentRepository(FakeAssetSource(emptyMap()))

        assertTrue(repo.index().isFailure)
    }

    // -- what the build actually carries --------------------------------------

    @Test
    fun `content the build does not carry is dropped from the index`() = runTest {
        // PDFs are not in the repository, so a plain checkout builds an APK
        // with the index but not the papers. Listing them would give a papers
        // screen where every row fails to open.
        val repo = repository(assets = bundled.filterKeys { !it.endsWith(".pdf") })

        assertEquals(emptyList<Any>(), repo.papers("cs"))
        assertEquals(4, repo.subjects("cs").first { it.id == "algo" }.topics.size)
    }

    @Test
    fun `a paper whose answer key is absent is still offered without one`() = runTest {
        val repo = repository(assets = bundled - "previousPapers/cs2024key.pdf")

        val paper = repo.papers("cs").first { it.id == "cs-2024" }
        assertNull(paper.answerKey)
    }

    @Test
    fun `a missing article is dropped rather than listed as a dead row`() = runTest {
        val repo = repository(assets = bundled - "algo/t4.html")

        val topics = repo.subjects("cs").first { it.id == "algo" }.topics
        assertEquals(listOf("t1", "t2", "t3"), topics.map { it.id })
        assertEquals(emptyList<Any>(), repo.search("cs", "Dijkstra"))
    }

    @Test
    fun `listing screens degrade to empty instead of throwing when the index is broken`() =
        runTest {
            val repo = repository("{ not json")

            assertEquals(emptyList<Any>(), repo.branches())
            assertEquals(emptyList<Any>(), repo.subjects("cs"))
            assertEquals(emptyList<Any>(), repo.papers("cs"))
            assertNull(repo.branch("cs"))
        }

    // -- branches and subjects ------------------------------------------------

    @Test
    fun `branches come back in their declared order`() = runTest {
        assertEquals(listOf("cs", "me"), repository().branches().map { it.id })
    }

    @Test
    fun `an unknown branch falls back to CS rather than a blank screen`() = runTest {
        assertEquals("cs", repository().branch("not-a-branch")?.id)
    }

    @Test
    fun `subjects are ordered by their paper order, not their id`() = runTest {
        assertEquals(listOf("aptitude", "algo"), repository().subjects("cs").map { it.id })
    }

    @Test
    fun `a subject with syllabus but no notes is marked as such`() = runTest {
        val aptitude = repository().subject("cs", "aptitude")

        assertTrue(aptitude!!.isSyllabusOnly)
        assertTrue("a syllabus is still something to show", !aptitude.isEmpty)
    }

    // -- papers ---------------------------------------------------------------

    @Test
    fun `papers are newest first and belong to the branch that asked`() = runTest {
        val papers = repository().papers("cs")

        assertEquals(listOf("cs-2024", "cs-2023"), papers.map { it.id })
    }

    @Test
    fun `a branch with no papers gets an empty list`() = runTest {
        assertEquals(emptyList<Any>(), repository().papers("me"))
    }

    // -- search ---------------------------------------------------------------

    @Test
    fun `search ranks a title that starts with the query above one that contains it`() = runTest {
        val hits = repository().search("cs", "bin")

        assertEquals(
            listOf("Binary Search Tree", "Complete Binary Tree", "Rebinning Histograms"),
            hits.map { it.topic.title },
        )
    }

    @Test
    fun `search is case insensitive`() = runTest {
        assertEquals(
            repository().search("cs", "bin").map { it.topic.id },
            repository().search("cs", "BIN").map { it.topic.id },
        )
    }

    @Test
    fun `search carries the subject each hit belongs to`() = runTest {
        val hit = repository().search("cs", "Dijkstra").single()

        assertEquals("algo", hit.subject.id)
        assertEquals("t4", hit.topic.id)
    }

    @Test
    fun `a single character is not a search`() = runTest {
        // Otherwise every keystroke of a real query first renders the whole
        // catalogue.
        assertEquals(emptyList<Any>(), repository().search("cs", "b"))
        assertEquals(emptyList<Any>(), repository().search("cs", " "))
    }

    @Test
    fun `search stays inside the chosen branch`() = runTest {
        assertEquals(emptyList<Any>(), repository().search("me", "binary"))
    }

    @Test
    fun `search respects its limit`() = runTest {
        assertEquals(1, repository().search("cs", "bin", limit = 1).size)
    }
}
