package com.gatemaster.app

import com.gatemaster.app.core.data.auth.ApiError
import com.gatemaster.app.core.data.auth.ApiResult
import com.gatemaster.app.core.data.auth.SyncApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The HTTP layer, against a mock engine.
 *
 * What is worth testing here is not that Ktor can make a request. It is the
 * behaviour the app adds around one: which requests carry a token, what happens
 * to a session when a refresh is refused, and whether an unreachable server is
 * distinguishable from one that said no -- because the app reacts differently
 * to each and getting them the wrong way round signs people out on a train.
 */
class SyncApiTest {

    private val json = HttpHeaders.ContentType to "application/json"

    // --- Sign in ------------------------------------------------------------

    @Test
    fun `login stores nothing itself but returns the session`() = runTest {
        val api = api { respondJson(SESSION_JSON) }
        val result = api.login("student@example.com", "correct-horse-battery")

        assertTrue(result is ApiResult.Ok)
        assertEquals("access-1", (result as ApiResult.Ok).value.accessToken)
    }

    @Test
    fun `an unreachable server is not the same as a rejection`() = runTest {
        // The distinction the whole error model exists for: no network means
        // keep the session and try later, a 401 means the session is gone.
        val api = api { throw java.io.IOException("no route to host") }

        val result = api.login("student@example.com", "correct-horse-battery")
        assertTrue((result as ApiResult.Failed).error is ApiError.Unreachable)
    }

    @Test
    fun `a rejected login carries the server's error code`() = runTest {
        val api = api {
            respondJson(
                """{"code":"invalid_credentials","message":"Email or password is incorrect"}""",
                HttpStatusCode.Unauthorized,
            )
        }

        // 401 on an auth endpoint is a wrong password, not an expired session,
        // so it must not be reported as SignedOut.
        val error = (api.login("a@example.com", "wrong") as ApiResult.Failed).error
        assertTrue(error is ApiError.Rejected)
        assertEquals("invalid_credentials", (error as ApiError.Rejected).body.code)
    }

    @Test
    fun `no configured server is a state, not a failure`() = runTest {
        val api = SyncApi(baseUrl = { null }, tokens = FakeSessionStore(), engine = MockEngine {
            error("should never be called")
        })

        val result = api.progress()
        assertEquals(ApiError.NotConfigured, (result as ApiResult.Failed).error)
    }

    // --- Bearer tokens ------------------------------------------------------

    @Test
    fun `sync requests carry the token and auth requests do not`() = runTest {
        val seen = mutableListOf<Pair<String, String?>>()
        val api = api(store = FakeSessionStore(storedSession())) { request ->
            seen += request.url.encodedPath to request.headers[HttpHeaders.Authorization]
            respondJson("""{"document":"{}","revision":3}""")
        }

        api.progress()
        assertEquals("Bearer access-1", seen.single().second)

        seen.clear()
        val loginApi = api(store = FakeSessionStore(storedSession())) { request ->
            seen += request.url.encodedPath to request.headers[HttpHeaders.Authorization]
            respondJson(SESSION_JSON)
        }
        loginApi.login("other@example.com", "password")
        // Signing in as somebody else must not carry the previous user's token.
        assertNull(seen.single().second)
    }

    @Test
    fun `a 401 triggers one refresh and the retried request succeeds`() = runTest {
        val store = FakeSessionStore(storedSession(accessToken = "expired"))
        var progressCalls = 0

        val api = api(store = store) { request ->
            when {
                request.url.encodedPath.endsWith("/auth/refresh") ->
                    respondJson(SESSION_JSON.replace("access-1", "access-2"))

                else -> {
                    progressCalls++
                    if (request.headers[HttpHeaders.Authorization] == "Bearer expired") {
                        respondError(HttpStatusCode.Unauthorized)
                    } else {
                        respondJson("""{"document":"{}","revision":9}""")
                    }
                }
            }
        }

        val result = api.progress()

        assertTrue(result is ApiResult.Ok)
        assertEquals(9L, (result as ApiResult.Ok).value.revision)
        // Once rejected, once retried -- not a loop.
        assertEquals(2, progressCalls)
        // And the rotated pair was persisted, or the next launch would start
        // from the token that was just spent.
        assertEquals("access-2", store.saved.last().accessToken)
    }

    @Test
    fun `a refused refresh drops the session instead of retrying forever`() = runTest {
        // This is reuse detection firing on the server, or a token that finally
        // expired. Either way this device cannot recover without the password.
        val store = FakeSessionStore(storedSession(accessToken = "expired"))
        val api = api(store = store) { request ->
            if (request.url.encodedPath.endsWith("/auth/refresh")) {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                respondError(HttpStatusCode.Unauthorized)
            }
        }

        val result = api.progress()

        assertTrue((result as ApiResult.Failed).error is ApiError.SignedOut)
        assertEquals(1, store.cleared)
        assertNull(store.current())
    }

    @Test
    fun `an unreachable refresh keeps the session`() = runTest {
        // Offline is not signed out. Clearing here would sign a user out for
        // going through a tunnel.
        val store = FakeSessionStore(storedSession(accessToken = "expired"))
        val api = api(store = store) { request ->
            if (request.url.encodedPath.endsWith("/auth/refresh")) {
                throw java.io.IOException("no route to host")
            } else {
                respondError(HttpStatusCode.Unauthorized)
            }
        }

        api.progress()

        assertEquals(0, store.cleared)
        assertEquals("expired", store.current()?.accessToken)
    }

    // --- Progress conflicts -------------------------------------------------

    @Test
    fun `a progress conflict comes back with the server's document`() = runTest {
        val api = api(store = FakeSessionStore(storedSession())) {
            respondJson(
                """{"code":"progress_conflict","message":"changed","current":{"document":"{\"read\":2}","revision":7}}""",
                HttpStatusCode.Conflict,
            )
        }

        val error = (api.putProgress("{}", 3) as ApiResult.Failed).error
        assertTrue(error is ApiError.Conflict)
        // Carried with the rejection so the client can merge without another
        // round trip it would otherwise always need.
        assertEquals(7L, (error as ApiError.Conflict).current.revision)
        assertEquals("""{"read":2}""", error.current.document)
    }

    @Test
    fun `a 409 that is not a progress conflict stays a rejection`() = runTest {
        // Registering an email that is taken is also a 409, and must not be
        // mistaken for a sync conflict.
        val api = api {
            respondJson(
                """{"code":"email_taken","message":"That email is already registered"}""",
                HttpStatusCode.Conflict,
            )
        }

        val error = (api.register("taken@example.com", "password12", "X") as ApiResult.Failed).error
        assertTrue(error is ApiError.Rejected)
        assertEquals("email_taken", (error as ApiError.Rejected).body.code)
    }

    // --- Plumbing -----------------------------------------------------------

    private fun api(
        store: FakeSessionStore = FakeSessionStore(),
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ) = SyncApi(
        baseUrl = { "https://sync.example.com" },
        tokens = store,
        engine = MockEngine { request -> handler(request) },
    )

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    private companion object {
        const val SESSION_JSON = """
            {
              "accessToken": "access-1",
              "accessTokenExpiresAt": 1767225600000,
              "refreshToken": "refresh-1",
              "user": {
                "id": "11111111-1111-1111-1111-111111111111",
                "email": "student@example.com",
                "displayName": "Student"
              }
            }
        """
    }
}
