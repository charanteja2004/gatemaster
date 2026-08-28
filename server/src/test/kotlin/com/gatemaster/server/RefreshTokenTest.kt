package com.gatemaster.server

import com.gatemaster.protocol.RefreshRequest
import com.gatemaster.protocol.SessionResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Duration

/**
 * Refresh-token rotation and what happens when a token is used twice.
 *
 * This is the part of the server most worth testing: it is security-relevant,
 * it is stateful, and every one of its rules is invisible in the happy path.
 */
class RefreshTokenTest {

    @Test
    fun `refresh returns a new pair and the old refresh token stops working`() = serverTest {
        val session = register()

        val refreshed: SessionResponse =
            client.postJson("/v1/auth/refresh", RefreshRequest(session.refreshToken)).body()

        assertNotEquals(session.refreshToken, refreshed.refreshToken)
        assertEquals(session.user.id, refreshed.user.id)

        // The new one works.
        assertEquals(
            HttpStatusCode.OK,
            client.get("/v1/me") { bearer(refreshed.accessToken) }.status,
        )

        // The old one does not: rotation spent it.
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson("/v1/auth/refresh", RefreshRequest(session.refreshToken)).status,
        )
    }

    @Test
    fun `reusing a spent token revokes the whole family`() = serverTest {
        // The scenario: a refresh token leaks. The thief refreshes with it, so
        // the real device's next refresh presents a token that has already been
        // spent -- or the other way round. Either way, one of them is a reuse,
        // and the server cannot tell which party is which.
        val session = register()

        val stolen = session.refreshToken
        val rotated: SessionResponse =
            client.postJson("/v1/auth/refresh", RefreshRequest(stolen)).body()

        // The second use of the spent token: detected.
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson("/v1/auth/refresh", RefreshRequest(stolen)).status,
        )

        // And the token the first use produced is dead too, even though it was
        // never itself reused. Both parties are signed out; only the one who
        // knows the password gets back in.
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson("/v1/auth/refresh", RefreshRequest(rotated.refreshToken)).status,
        )
    }

    @Test
    fun `revoking one family leaves another sign-in alone`() = serverTest {
        // Two devices, two sign-ins, two families. Compromising one must not
        // sign the other out -- which is why a family is per sign-in and not
        // per user.
        val phone = register()
        val tablet: SessionResponse = login().body()

        val stolen = phone.refreshToken
        client.postJson("/v1/auth/refresh", RefreshRequest(stolen))
        client.postJson("/v1/auth/refresh", RefreshRequest(stolen)) // reuse: kills the phone's family

        assertEquals(
            HttpStatusCode.OK,
            client.postJson("/v1/auth/refresh", RefreshRequest(tablet.refreshToken)).status,
        )
    }

    @Test
    fun `an expired refresh token is refused`() = serverTest {
        val session = register()
        clock.advance(Duration.ofDays(61)) // the configured lifetime is 60

        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson("/v1/auth/refresh", RefreshRequest(session.refreshToken)).status,
        )
    }

    @Test
    fun `an expired access token is refused but refresh still works`() = serverTest {
        val session = register()
        clock.advance(Duration.ofMinutes(16)) // access tokens live 15

        assertEquals(
            HttpStatusCode.Unauthorized,
            client.get("/v1/me") { bearer(session.accessToken) }.status,
        )

        // This is the whole point of the split: the long-lived credential is
        // still good, so the user is not signed out by a short expiry.
        val refreshed: SessionResponse =
            client.postJson("/v1/auth/refresh", RefreshRequest(session.refreshToken)).body()
        assertEquals(
            HttpStatusCode.OK,
            client.get("/v1/me") { bearer(refreshed.accessToken) }.status,
        )
    }

    @Test
    fun `logout revokes only the device that asked`() = serverTest {
        val phone = register()
        val tablet: SessionResponse = login().body()

        assertEquals(
            HttpStatusCode.NoContent,
            client.postJson("/v1/auth/logout", RefreshRequest(phone.refreshToken)).status,
        )

        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson("/v1/auth/refresh", RefreshRequest(phone.refreshToken)).status,
        )
        assertEquals(
            HttpStatusCode.OK,
            client.postJson("/v1/auth/refresh", RefreshRequest(tablet.refreshToken)).status,
        )
    }

    @Test
    fun `logging out an unknown token still reports success`() = serverTest {
        // Otherwise this endpoint answers "does this token exist?" for anyone
        // who asks, and the caller's intent -- be signed out -- is satisfied
        // either way.
        assertEquals(
            HttpStatusCode.NoContent,
            client.postJson("/v1/auth/logout", RefreshRequest("never-issued")).status,
        )
    }

    @Test
    fun `an unknown refresh token is refused`() = serverTest {
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson("/v1/auth/refresh", RefreshRequest("never-issued")).status,
        )
    }
}
