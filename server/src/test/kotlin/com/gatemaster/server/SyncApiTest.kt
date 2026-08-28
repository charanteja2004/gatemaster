package com.gatemaster.server

import com.gatemaster.protocol.ProgressConflictResponse
import com.gatemaster.protocol.ProgressPutRequest
import com.gatemaster.protocol.ProgressResponse
import com.gatemaster.protocol.UploadAttemptsRequest
import com.gatemaster.protocol.AttemptPage
import com.gatemaster.protocol.SyncedAttempt
import com.gatemaster.protocol.SyncedAttemptQuestion
import com.gatemaster.protocol.UploadResult
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncApiTest {

    // --- Study progress: optimistic concurrency -----------------------------

    @Test
    fun `a user who has never synced reads revision zero`() = serverTest {
        val session = register()
        val progress: ProgressResponse =
            client.get("/v1/sync/progress") { bearer(session.accessToken) }.body()

        // Not a 404: revision 0 is the state a first write must be based on, so
        // the client needs a real answer here rather than an error to interpret.
        assertEquals(0L, progress.revision)
        assertEquals("", progress.document)
    }

    @Test
    fun `writing progress advances the revision`() = serverTest {
        val session = register()

        val first: ProgressResponse = client.putProgress(session.accessToken, """{"read":1}""", 0).body()
        assertEquals(1L, first.revision)

        val second: ProgressResponse = client.putProgress(session.accessToken, """{"read":2}""", 1).body()
        assertEquals(2L, second.revision)

        val read: ProgressResponse =
            client.get("/v1/sync/progress") { bearer(session.accessToken) }.body()
        assertEquals("""{"read":2}""", read.document)
    }

    @Test
    fun `a write from a stale revision is rejected and hands back the current state`() = serverTest {
        // Two devices. Both read revision 1, both edit, both write. The second
        // must not silently erase the first -- which is exactly what a plain
        // last-write-wins PUT would do to a week of reading history.
        val session = register()
        client.putProgress(session.accessToken, """{"read":["os"]}""", 0)

        val phone = client.putProgress(session.accessToken, """{"read":["os","dbms"]}""", 1)
        assertEquals(HttpStatusCode.OK, phone.status)

        val tablet = client.putProgress(session.accessToken, """{"read":["os","algo"]}""", 1)
        assertEquals(HttpStatusCode.Conflict, tablet.status)

        val conflict: ProgressConflictResponse = tablet.body()
        assertEquals("progress_conflict", conflict.code)
        // The rejection carries what the server holds, so the client can merge
        // without a second round trip it would otherwise always need.
        assertEquals(2L, conflict.current.revision)
        assertEquals("""{"read":["os","dbms"]}""", conflict.current.document)
    }

    @Test
    fun `a first write claiming a revision it cannot have is rejected`() = serverTest {
        val session = register()
        val response = client.putProgress(session.accessToken, """{"read":[]}""", 7)
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `progress is private to its owner`() = serverTest {
        val mine = register(email = "mine@example.com")
        client.putProgress(mine.accessToken, """{"read":["os"]}""", 0)

        val theirs = register(email = "theirs@example.com")
        val read: ProgressResponse =
            client.get("/v1/sync/progress") { bearer(theirs.accessToken) }.body()
        assertEquals(0L, read.revision)
        assertEquals("", read.document)
    }

    @Test
    fun `sync refuses an unauthenticated caller`() = serverTest {
        assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/sync/progress").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/sync/attempts").status)
    }

    // --- Attempts: append-only and idempotent -------------------------------

    @Test
    fun `uploading attempts stores them with their questions`() = serverTest {
        val session = register()
        val result: UploadResult = client.postJson(
            "/v1/sync/attempts",
            UploadAttemptsRequest(listOf(attempt("a1"), attempt("a2"))),
        ) { bearer(session.accessToken) }.body()

        assertEquals(2, result.accepted)
        assertTrue(result.duplicates.isEmpty())

        val page: AttemptPage =
            client.get("/v1/sync/attempts") { bearer(session.accessToken) }.body()
        assertEquals(2, page.attempts.size)
        assertEquals(2, page.attempts.first().questions.size)
        assertEquals("os", page.attempts.first().questions.first().subjectId)
    }

    @Test
    fun `uploading the same attempt twice stores it once`() = serverTest {
        // The case this exists for: the upload succeeds, the response is lost
        // to a dropped connection, and the client retries. Without the client
        // id as an idempotency key, that retry doubles the attempt and every
        // average computed from it.
        val session = register()
        val body = UploadAttemptsRequest(listOf(attempt("a1")))

        val first: UploadResult =
            client.postJson("/v1/sync/attempts", body) { bearer(session.accessToken) }.body()
        val retry: UploadResult =
            client.postJson("/v1/sync/attempts", body) { bearer(session.accessToken) }.body()

        assertEquals(1, first.accepted)
        assertEquals(0, retry.accepted)
        assertEquals(listOf("a1"), retry.duplicates)

        val page: AttemptPage =
            client.get("/v1/sync/attempts") { bearer(session.accessToken) }.body()
        assertEquals(1, page.attempts.size)
    }

    @Test
    fun `one duplicate in a batch does not block the rest`() = serverTest {
        val session = register()
        client.postJson("/v1/sync/attempts", UploadAttemptsRequest(listOf(attempt("a1")))) {
            bearer(session.accessToken)
        }

        val result: UploadResult = client.postJson(
            "/v1/sync/attempts",
            UploadAttemptsRequest(listOf(attempt("a1"), attempt("a2"), attempt("a3"))),
        ) { bearer(session.accessToken) }.body()

        assertEquals(2, result.accepted)
        assertEquals(listOf("a1"), result.duplicates)
    }

    @Test
    fun `download resumes from the sequence the caller already holds`() = serverTest {
        val session = register()
        client.postJson(
            "/v1/sync/attempts",
            UploadAttemptsRequest(listOf(attempt("a1"), attempt("a2"), attempt("a3"))),
        ) { bearer(session.accessToken) }

        val all: AttemptPage =
            client.get("/v1/sync/attempts") { bearer(session.accessToken) }.body()
        assertEquals(3, all.attempts.size)

        val cursor = all.attempts[0].serverSeq!!
        val rest: AttemptPage =
            client.get("/v1/sync/attempts?since=$cursor") { bearer(session.accessToken) }.body()
        assertEquals(2, rest.attempts.size)
        assertEquals(listOf("a2", "a3"), rest.attempts.map { it.clientAttemptId })
    }

    @Test
    fun `a full page advertises a cursor and a short page does not`() = serverTest {
        val session = register()
        client.postJson(
            "/v1/sync/attempts",
            UploadAttemptsRequest(listOf(attempt("a1"), attempt("a2"), attempt("a3"))),
        ) { bearer(session.accessToken) }

        val full: AttemptPage =
            client.get("/v1/sync/attempts?limit=2") { bearer(session.accessToken) }.body()
        assertEquals(2, full.attempts.size)
        assertEquals(full.attempts.last().serverSeq, full.nextSince)

        // A page that did not fill means there is nothing after it, and saying
        // so stops the client asking for a page it can already tell is empty.
        val last: AttemptPage =
            client.get("/v1/sync/attempts?limit=2&since=${full.nextSince}") {
                bearer(session.accessToken)
            }.body()
        assertEquals(1, last.attempts.size)
        assertNull(last.nextSince)
    }

    @Test
    fun `attempts are private to their owner`() = serverTest {
        val mine = register(email = "mine@example.com")
        client.postJson("/v1/sync/attempts", UploadAttemptsRequest(listOf(attempt("a1")))) {
            bearer(mine.accessToken)
        }

        val theirs = register(email = "theirs@example.com")
        val page: AttemptPage =
            client.get("/v1/sync/attempts") { bearer(theirs.accessToken) }.body()
        assertTrue(page.attempts.isEmpty())

        // And the same client id is free for another user to use.
        val result: UploadResult = client.postJson(
            "/v1/sync/attempts",
            UploadAttemptsRequest(listOf(attempt("a1"))),
        ) { bearer(theirs.accessToken) }.body()
        assertEquals(1, result.accepted)
    }
}

private suspend fun io.ktor.client.HttpClient.putProgress(
    token: String,
    document: String,
    revision: Long,
) = put("/v1/sync/progress") {
    bearer(token)
    contentType(ContentType.Application.Json)
    setBody(ProgressPutRequest(document, revision))
}

private fun attempt(clientId: String) = SyncedAttempt(
    clientAttemptId = clientId,
    testId = "practice:subject:os",
    testTitle = "Operating Systems",
    startedAt = 1_767_225_600_000,
    finishedAt = 1_767_227_400_000,
    durationSeconds = 1_800,
    score = 12.0,
    maxScore = 20.0,
    questions = listOf(
        SyncedAttemptQuestion(
            questionId = "os-1",
            subjectId = "os",
            topicId = "scheduling",
            questionType = "MCQ",
            marks = 2.0,
            awarded = 2.0,
            wasAttempted = true,
            wasCorrect = true,
        ),
        SyncedAttemptQuestion(
            questionId = "os-2",
            subjectId = "os",
            topicId = "deadlock",
            questionType = "MSQ",
            marks = 2.0,
            awarded = 0.0,
            wasAttempted = true,
            wasCorrect = false,
        ),
    ),
)
