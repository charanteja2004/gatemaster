package com.gatemaster.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.gatemaster.server.Config
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * The two kinds of token this server issues, and the asymmetry between them.
 *
 * The **access token** is a JWT. It is self-describing, so every request can be
 * authorised without touching the database, and it cannot be revoked -- which
 * is why it lives for fifteen minutes.
 *
 * The **refresh token** is an opaque random string. It carries no claims, means
 * nothing without the row it points at, and can be revoked instantly, which is
 * why it is the one allowed to live for months.
 */
class Tokens(
    private val config: Config,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(config.jwtSecret)
    private val random = SecureRandom()

    /**
     * Built against [clock], not the system clock.
     *
     * java-jwt would otherwise check `exp` against real time while these tokens
     * were minted from an injected one, so under test every freshly issued
     * token would look expired. In production both are the system clock and
     * this changes nothing.
     *
     * The cast is needed because `build(Clock)` is declared on the concrete
     * BaseVerification rather than on the Verification interface that
     * `JWT.require` returns. There is no other way to reach it, and the
     * alternative -- re-checking expiry by hand in the validate block -- would
     * mean two implementations of the same rule.
     */
    val verifier: JWTVerifier = (
        JWT.require(algorithm)
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience) as JWTVerifier.BaseVerification
        ).build(clock)

    fun issueAccessToken(userId: UUID): AccessToken {
        val issuedAt = clock.instant()
        val expiresAt = issuedAt.plus(config.accessTokenLifetime)
        val jwt = JWT.create()
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withSubject(userId.toString())
            .withIssuedAt(issuedAt)
            .withExpiresAt(expiresAt)
            .sign(algorithm)
        return AccessToken(jwt, expiresAt)
    }

    /**
     * A fresh opaque refresh token: 32 bytes of [SecureRandom], URL-safe.
     *
     * The plaintext is returned to the caller once and never stored. What the
     * database keeps is [hash] of it, so the table is useless to anyone who
     * reads it.
     */
    fun issueRefreshToken(): RefreshTokenValue {
        val bytes = ByteArray(REFRESH_TOKEN_BYTES).also(random::nextBytes)
        val value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return RefreshTokenValue(
            value = value,
            hash = hash(value),
            expiresAt = clock.instant().plus(config.refreshTokenLifetime),
        )
    }

    fun now(): Instant = clock.instant()

    companion object {
        private const val REFRESH_TOKEN_BYTES = 32

        /**
         * SHA-256, hex, lower-case -- fixed at 64 characters, which is what the
         * CHAR(64) column expects.
         *
         * No salt and no work factor, unlike a password: this input is already
         * 256 bits of entropy from SecureRandom, so there is no dictionary to
         * attack and nothing for a slow hash to buy.
         */
        fun hash(token: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(token.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}

data class AccessToken(val value: String, val expiresAt: Instant)

data class RefreshTokenValue(
    val value: String,
    val hash: String,
    val expiresAt: Instant,
)
