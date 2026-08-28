package com.gatemaster.server

import com.gatemaster.server.api.LoginRequest
import com.gatemaster.server.api.RegisterRequest
import com.gatemaster.server.api.SessionResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * A clock the tests can move.
 *
 * Token expiry is a time-dependent rule, and the only alternative to
 * controlling time is sleeping through it -- which would make the suite slow
 * and, worse, flaky on a loaded machine.
 */
class MutableClock(private var instant: Instant = Instant.parse("2026-01-01T00:00:00Z")) : Clock() {
    override fun instant(): Instant = instant
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    fun advance(by: Duration) { instant = instant.plus(by) }
}

/**
 * Runs [body] against a real server on a real (in-memory) database.
 *
 * These are integration tests on purpose. The interesting behaviour of this
 * server -- token rotation, optimistic concurrency, idempotent upload -- lives
 * in how the routes, the service and the SQL fit together, and a unit test with
 * the database faked out would assert the parts while proving nothing about the
 * join between them.
 */
fun serverTest(
    clock: MutableClock = MutableClock(),
    body: suspend ServerTestScope.() -> Unit,
) = runTest {
    TestDatabase.fresh().use { database ->
        testApplication {
            application { module(database, TestDatabase.config(), clock) }
            val client = createClient {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
            ServerTestScope(this, client, clock).body()
        }
    }
}

class ServerTestScope(
    val builder: ApplicationTestBuilder,
    val client: HttpClient,
    val clock: MutableClock,
) {
    /** Registers a user and returns the session, for tests that need one signed in. */
    suspend fun register(
        email: String = "student@example.com",
        password: String = "correct-horse-battery",
        displayName: String = "Student",
    ): SessionResponse = client.postJson(
        "/v1/auth/register",
        RegisterRequest(email, password, displayName),
    ).body()

    suspend fun login(
        email: String = "student@example.com",
        password: String = "correct-horse-battery",
    ): HttpResponse = client.postJson("/v1/auth/login", LoginRequest(email, password))
}

suspend inline fun <reified T> HttpClient.postJson(
    url: String,
    payload: T,
    noinline configure: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = post(url) {
    contentType(ContentType.Application.Json)
    setBody(payload)
    configure()
}

/** Adds the bearer token every authenticated route expects. */
fun HttpRequestBuilder.bearer(token: String) {
    header(HttpHeaders.Authorization, "Bearer $token")
}
