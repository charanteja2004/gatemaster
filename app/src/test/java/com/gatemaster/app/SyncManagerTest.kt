package com.gatemaster.app

import androidx.room.Room
import com.gatemaster.app.core.data.StudyProgressRepository
import com.gatemaster.app.core.data.TopicProgress
import com.gatemaster.app.core.data.auth.SyncApi
import com.gatemaster.app.core.data.db.AttemptEntity
import com.gatemaster.app.core.data.db.GateMasterDatabase
import com.gatemaster.app.core.data.db.QuestionResultEntity
import com.gatemaster.app.core.data.sync.SyncManager
import com.gatemaster.app.core.data.sync.SyncOutcome
import com.gatemaster.protocol.AttemptPage
import com.gatemaster.protocol.SyncedAttempt
import com.gatemaster.protocol.ProgressPutRequest
import com.gatemaster.protocol.UploadResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * A full sync cycle, against a real database and a scripted server.
 *
 * The rules worth proving here are the ones that only show up across a whole
 * round trip: that an upload is not repeated, that the download cursor actually
 * advances past the rows this device sent, and that a rejected progress write
 * merges and retries instead of losing what was read.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncManagerTest {

    private lateinit var db: GateMasterDatabase
    private lateinit var filesDir: File
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Every request the server saw, so a test can assert what was not sent. */
    private val requests = mutableListOf<HttpRequestData>()

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GateMasterDatabase::class.java,
        ).allowMainThreadQueries().build()
        filesDir = File.createTempFile("gatemaster", "").let { temp ->
            temp.delete()
            temp.mkdirs()
            temp
        }
    }

    @After
    fun close() {
        db.close()
        filesDir.deleteRecursively()
    }

    // --- Attempts -----------------------------------------------------------

    @Test
    fun `unsynced attempts are uploaded once and then left alone`() = runTest {
        recordLocalAttempt("a1")
        recordLocalAttempt("a2")

        var uploads = 0
        val manager = manager { request ->
            when {
                request.isUpload() -> {
                    uploads++
                    respondJson(json.encodeToString(UploadResult(accepted = 2, highestSeq = 2)))
                }
                request.isAttemptDownload() -> respondJson(json.encodeToString(AttemptPage()))
                else -> progressResponses()
            }
        }

        val first = manager.sync()
        assertTrue(first is SyncOutcome.Success)
        assertEquals(2, (first as SyncOutcome.Success).attemptsUploaded)

        // Second run: both rows are marked synced, so there is nothing to send
        // and the server is not asked again.
        manager.sync()
        assertEquals(1, uploads)
    }

    @Test
    fun `attempts the server already had are still marked synced`() = runTest {
        // Otherwise the same rejected rows are retried on every sync for ever.
        recordLocalAttempt("a1")

        var uploads = 0
        val manager = manager { request ->
            when {
                request.isUpload() -> {
                    uploads++
                    respondJson(
                        json.encodeToString(
                            UploadResult(accepted = 0, duplicates = listOf("a1"), highestSeq = 1),
                        ),
                    )
                }
                request.isAttemptDownload() -> respondJson(json.encodeToString(AttemptPage()))
                else -> progressResponses()
            }
        }

        manager.sync()
        manager.sync()

        assertEquals(1, uploads)
        assertTrue(db.attemptDao().unsyncedAttempts(10).isEmpty())
    }

    @Test
    fun `an attempt from another device is stored, and not re-stored`() = runTest {
        var downloads = 0
        val manager = manager { request ->
            when {
                request.isUpload() -> respondJson(json.encodeToString(UploadResult(0)))
                request.isAttemptDownload() -> {
                    downloads++
                    respondJson(
                        json.encodeToString(
                            AttemptPage(attempts = listOf(remoteAttempt("from-tablet", seq = 7))),
                        ),
                    )
                }
                else -> progressResponses()
            }
        }

        val first = manager.sync() as SyncOutcome.Success
        assertEquals(1, first.attemptsDownloaded)

        // The server keeps offering it; the client must recognise it as one it
        // already holds rather than inserting a second copy.
        val second = manager.sync() as SyncOutcome.Success
        assertEquals(0, second.attemptsDownloaded)
        assertEquals(1, db.attemptDao().count())
        assertEquals(2, downloads)
    }

    @Test
    fun `the download cursor moves past the rows this device uploaded`() = runTest {
        // The bug this pins down: an uploaded attempt comes back down on the
        // next page, is recognised as known, and is skipped -- and if that skip
        // does not record the server's sequence, MAX(serverSeq) never moves and
        // the same page is fetched on every sync for the life of the install.
        recordLocalAttempt("a1")

        val since = mutableListOf<String?>()
        val manager = manager { request ->
            when {
                request.isUpload() ->
                    respondJson(json.encodeToString(UploadResult(accepted = 1, highestSeq = 4)))

                request.isAttemptDownload() -> {
                    since += request.url.parameters["since"]
                    respondJson(
                        json.encodeToString(
                            AttemptPage(attempts = listOf(remoteAttempt("a1", seq = 4))),
                        ),
                    )
                }

                else -> progressResponses()
            }
        }

        manager.sync()
        manager.sync()

        assertEquals("0", since.first())
        assertEquals("4", since.last())
    }

    // --- Study progress -----------------------------------------------------

    @Test
    fun `the server's reading history is merged into this device's`() = runTest {
        val progress = studyProgress()
        progress.load()
        progress.merge(mapOf("os-paging" to topic("os-paging", furthest = 0.2f, lastOpened = 100)))

        var pushed: String? = null
        val manager = manager(progress) { request ->
            when {
                request.isUpload() -> respondJson(json.encodeToString(UploadResult(0)))
                request.isAttemptDownload() -> respondJson(json.encodeToString(AttemptPage()))
                request.method == HttpMethod.Put -> {
                    pushed = request.bodyText()
                    respondJson("""{"document":"{}","revision":2}""")
                }
                else -> {
                    val remote = json.encodeToString(
                        mapOf("dbms-joins" to topic("dbms-joins", furthest = 0.9f, lastOpened = 300)),
                    )
                    respondJson("""{"document":${json.encodeToString(remote)},"revision":1}""")
                }
            }
        }

        val outcome = manager.sync() as SyncOutcome.Success
        assertTrue(outcome.progressPulled)
        assertTrue(outcome.progressPushed)

        // Both topics survive locally...
        assertEquals(setOf("os-paging", "dbms-joins"), progress.progress.value.keys)
        // ...and both were pushed back, so the other device gets ours too.
        assertTrue(pushed!!.contains("os-paging"))
        assertTrue(pushed!!.contains("dbms-joins"))
    }

    @Test
    fun `a rejected write merges against the conflict and pushes once more`() = runTest {
        val progress = studyProgress()
        progress.load()
        progress.merge(mapOf("os-paging" to topic("os-paging", furthest = 0.5f, lastOpened = 100)))

        val revisionsSent = mutableListOf<Long>()
        var puts = 0
        val manager = manager(progress) { request ->
            when {
                request.isUpload() -> respondJson(json.encodeToString(UploadResult(0)))
                request.isAttemptDownload() -> respondJson(json.encodeToString(AttemptPage()))
                request.method == HttpMethod.Put -> {
                    puts++
                    revisionsSent += json.decodeFromString<ProgressPutRequest>(
                        request.bodyText(),
                    ).revision

                    if (puts == 1) {
                        // Another device wrote in between. The rejection carries
                        // what it wrote, which is what the retry has to merge.
                        val theirs = json.encodeToString(
                            mapOf("cd-parsing" to topic("cd-parsing", furthest = 1f, lastOpened = 400)),
                        )
                        respondJson(
                            """{"code":"progress_conflict","message":"x","current":{"document":${json.encodeToString(theirs)},"revision":9}}""",
                            HttpStatusCode.Conflict,
                        )
                    } else {
                        respondJson("""{"document":"{}","revision":10}""")
                    }
                }
                else -> respondJson("""{"document":"","revision":0}""")
            }
        }

        val outcome = manager.sync() as SyncOutcome.Success

        assertTrue(outcome.progressPushed)
        assertEquals(2, puts)
        // The retry uses the revision the server said it was on, not the stale
        // one -- sending the stale revision again would conflict for ever.
        assertEquals(listOf(0L, 9L), revisionsSent)
        // And nothing was lost: the other device's topic is now here too.
        assertEquals(setOf("os-paging", "cd-parsing"), progress.progress.value.keys)
    }

    @Test
    fun `nothing is sent when the server already holds the same document`() = runTest {
        // Two idle devices must not bounce revisions off each other for ever.
        val progress = studyProgress()
        progress.load()
        val local = mapOf("os-paging" to topic("os-paging", furthest = 0.5f, lastOpened = 100))
        progress.merge(local)

        var puts = 0
        val manager = manager(progress) { request ->
            when {
                request.isUpload() -> respondJson(json.encodeToString(UploadResult(0)))
                request.isAttemptDownload() -> respondJson(json.encodeToString(AttemptPage()))
                request.method == HttpMethod.Put -> {
                    puts++
                    respondJson("""{"document":"{}","revision":2}""")
                }
                else -> respondJson(
                    """{"document":${json.encodeToString(json.encodeToString(local))},"revision":1}""",
                )
            }
        }

        val outcome = manager.sync() as SyncOutcome.Success
        assertEquals(0, puts)
        assertTrue(outcome.progressPulled)
    }

    // --- Signed out ---------------------------------------------------------

    @Test
    fun `nothing happens when nobody is signed in`() = runTest {
        val manager = SyncManager(
            api = SyncApi(
                baseUrl = { "https://sync.example.com" },
                tokens = FakeSessionStore(),
                engine = MockEngine { error("must not be called") },
            ),
            studyProgress = studyProgress(),
            dao = db.attemptDao(),
            tokens = FakeSessionStore(),
        )

        assertEquals(SyncOutcome.NothingToDo, manager.sync())
    }

    // --- Plumbing -----------------------------------------------------------

    private fun studyProgress() = StudyProgressRepository(filesDir)

    private fun manager(
        progress: StudyProgressRepository = studyProgress(),
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): SyncManager {
        val store = FakeSessionStore(storedSession())
        val api = SyncApi(
            baseUrl = { "https://sync.example.com" },
            tokens = store,
            engine = MockEngine { request ->
                requests += request
                handler(request)
            },
        )
        return SyncManager(
            api = api,
            studyProgress = progress,
            dao = db.attemptDao(),
            tokens = store,
            now = { 1_000L },
        )
    }

    private suspend fun recordLocalAttempt(clientId: String) {
        db.attemptDao().record(
            attempt = AttemptEntity(
                testId = "practice:subject:os",
                title = "Operating Systems",
                submittedAtEpochMs = 1_700_000_000_000,
                score = 12.0,
                maxMarks = 20,
                correct = 6,
                incorrect = 2,
                unattempted = 2,
                timeTakenMs = 1_800_000,
                clientAttemptId = clientId,
            ),
            results = listOf(
                QuestionResultEntity(
                    attemptId = 0,
                    questionId = "os-1",
                    subjectId = "os",
                    topicId = "scheduling",
                    marks = 2,
                    marksAwarded = 2.0,
                    kind = "CORRECT",
                ),
            ),
        )
    }

    private fun remoteAttempt(clientId: String, seq: Long) = SyncedAttempt(
        clientAttemptId = clientId,
        testId = "practice:subject:dbms",
        testTitle = "Databases",
        startedAt = 1_700_000_000_000,
        finishedAt = 1_700_000_600_000,
        durationSeconds = 600,
        score = 8.0,
        maxScore = 20.0,
        serverSeq = seq,
    )

    private fun topic(id: String, furthest: Float = 0f, lastOpened: Long = 0) = TopicProgress(
        topicId = id,
        branchId = "cs",
        subjectId = id.substringBefore('-'),
        subjectName = "Subject",
        title = "Topic",
        path = "$id.html",
        lastOpenedEpochMs = lastOpened,
        furthest = furthest,
    )

    private fun HttpRequestData.isUpload() =
        method == HttpMethod.Post && url.encodedPath.endsWith("/sync/attempts")

    private fun HttpRequestData.isAttemptDownload() =
        method == HttpMethod.Get && url.encodedPath.endsWith("/sync/attempts")

    /** MockEngine's own reader, which handles every OutgoingContent shape. */
    private suspend fun HttpRequestData.bodyText(): String = String(body.toByteArray())

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.progressResponses() =
        respondJson("""{"document":"","revision":0}""")

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
}
