package com.gatemaster.server

import com.gatemaster.protocol.ErrorResponse
import com.gatemaster.protocol.LoginRequest
import com.gatemaster.protocol.RegisterRequest
import com.gatemaster.protocol.SessionResponse
import com.gatemaster.protocol.UserResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthApiTest {

    @Test
    fun `register returns a session and creates the account`() = serverTest {
        val response = client.postJson(
            "/v1/auth/register",
            RegisterRequest("Student@Example.com", "correct-horse-battery", "Charan"),
        )
        assertEquals(HttpStatusCode.Created, response.status)

        val session: SessionResponse = response.body()
        assertEquals("Charan", session.user.displayName)
        // Stored lower-cased, so signing in with different capitalisation works.
        assertEquals("student@example.com", session.user.email)
        assertTrue(session.accessToken.isNotEmpty())
        assertTrue(session.refreshToken.isNotEmpty())
    }

    @Test
    fun `an email registers only once`() = serverTest {
        register(email = "taken@example.com")
        val second = client.postJson(
            "/v1/auth/register",
            RegisterRequest("TAKEN@example.com", "another-password", "Someone Else"),
        )
        assertEquals(HttpStatusCode.Conflict, second.status)
        assertEquals("email_taken", second.body<ErrorResponse>().code)
    }

    @Test
    fun `login accepts the password and any capitalisation of the email`() = serverTest {
        register(email = "student@example.com", password = "correct-horse-battery")
        val response = login(email = "STUDENT@Example.com", password = "correct-horse-battery")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `a wrong password and an unknown account are the same answer`() = serverTest {
        register(email = "real@example.com", password = "correct-horse-battery")

        val wrongPassword = login(email = "real@example.com", password = "not-the-password")
        val noSuchUser = login(email = "ghost@example.com", password = "correct-horse-battery")

        // Identical status and identical body. Any difference between these two
        // is a way to find out who has an account.
        assertEquals(HttpStatusCode.Unauthorized, wrongPassword.status)
        assertEquals(HttpStatusCode.Unauthorized, noSuchUser.status)
        assertEquals(
            wrongPassword.body<ErrorResponse>().code,
            noSuchUser.body<ErrorResponse>().code,
        )
        assertEquals(
            wrongPassword.body<ErrorResponse>().message,
            noSuchUser.body<ErrorResponse>().message,
        )
    }

    @Test
    fun `a short password is rejected with the field named`() = serverTest {
        val response = client.postJson(
            "/v1/auth/register",
            RegisterRequest("short@example.com", "abc", "Short"),
        )
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = response.body<ErrorResponse>()
        assertEquals("validation_failed", error.code)
        assertEquals("password", error.field)
    }

    @Test
    fun `a malformed email is rejected`() = serverTest {
        val response = client.postJson(
            "/v1/auth/register",
            RegisterRequest("not-an-email", "correct-horse-battery", "Nobody"),
        )
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("email", response.body<ErrorResponse>().field)
    }

    @Test
    fun `a very long password is accepted rather than truncated`() = serverTest {
        // BCrypt reads 72 bytes. Without the pre-hash in Passwords, this would
        // either throw or quietly ignore everything past the 72nd character --
        // which would make the two passwords below equivalent.
        val long = "x".repeat(150)
        client.postJson("/v1/auth/register", RegisterRequest("long@example.com", long, "Long"))

        val correct = client.postJson("/v1/auth/login", LoginRequest("long@example.com", long))
        assertEquals(HttpStatusCode.OK, correct.status)

        val truncatedAt72 = client.postJson(
            "/v1/auth/login",
            LoginRequest("long@example.com", "x".repeat(72)),
        )
        assertEquals(HttpStatusCode.Unauthorized, truncatedAt72.status)
    }

    @Test
    fun `me returns the signed-in user`() = serverTest {
        val session = register(displayName = "Charan")
        val response = client.get("/v1/me") { bearer(session.accessToken) }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Charan", response.body<UserResponse>().displayName)
    }

    @Test
    fun `me refuses a missing, malformed or foreign token`() = serverTest {
        assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/me").status)

        assertEquals(
            HttpStatusCode.Unauthorized,
            client.get("/v1/me") { bearer("not-a-jwt") }.status,
        )

        // Correctly formed and correctly signed -- but by a different secret.
        val foreign = com.auth0.jwt.JWT.create()
            .withIssuer("gatemaster-test")
            .withAudience("gatemaster-test-app")
            .withSubject(java.util.UUID.randomUUID().toString())
            .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256("a-different-secret-entirely-x"))
        assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/me") { bearer(foreign) }.status)
    }

    @Test
    fun `two sign-ins get different tokens`() = serverTest {
        val first = register()
        val second: SessionResponse = login().body()
        assertNotEquals(first.refreshToken, second.refreshToken)
        assertEquals(first.user.id, second.user.id)
    }

    @Test
    fun `health needs no token`() = serverTest {
        assertEquals(HttpStatusCode.OK, client.get("/health").status)
    }
}
