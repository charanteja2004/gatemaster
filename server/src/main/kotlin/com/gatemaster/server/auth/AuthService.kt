package com.gatemaster.server.auth

import java.util.UUID

/** A signed-in session: what every successful auth call hands back. */
data class Session(
    val user: User,
    val accessToken: AccessToken,
    val refreshToken: RefreshTokenValue,
)

/** The credentials were wrong, or the account does not exist. Deliberately one case. */
class InvalidCredentials : Exception("Email or password is incorrect")

/** The refresh token is unknown, expired, or has already been used. */
class InvalidRefreshToken : Exception("Sign in again")

/** The request was rejected before it reached the database. */
class ValidationFailed(val field: String, override val message: String) : Exception(message)

class AuthService(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val passwords: Passwords,
    private val tokens: Tokens,
) {

    fun register(email: String, password: String, displayName: String): Session {
        val cleanEmail = validateEmail(email)
        validatePassword(password)
        val cleanName = displayName.trim().ifEmpty { cleanEmail.substringBefore('@') }
        if (cleanName.length > MAX_DISPLAY_NAME) {
            throw ValidationFailed("displayName", "Name must be $MAX_DISPLAY_NAME characters or fewer")
        }

        val now = tokens.now()
        val user = users.create(cleanEmail, passwords.hash(password), cleanName, now)
        return startSession(user.id, user, now)
    }

    fun login(email: String, password: String): Session {
        val found = users.findByEmailWithHash(email)

        if (found == null) {
            // Hash anyway. Returning early here would make a request for an
            // unknown address measurably faster than one for a known address,
            // which turns this endpoint into a way to enumerate who has an
            // account. The wasted work is the point.
            passwords.verify(password, DUMMY_HASH)
            throw InvalidCredentials()
        }

        val (user, hash) = found
        if (!passwords.verify(password, hash)) throw InvalidCredentials()

        return startSession(user.id, user, tokens.now())
    }

    /**
     * Exchanges a refresh token for a new pair, rotating it.
     *
     * The presented token is always invalidated -- either it is spent normally,
     * or it was already spent and the whole family goes with it.
     */
    fun refresh(refreshToken: String): Session {
        val now = tokens.now()
        val hash = Tokens.hash(refreshToken)
        val stored = refreshTokens.findByHash(hash) ?: throw InvalidRefreshToken()

        if (stored.revokedAt != null) {
            // Rotation makes every token single-use, so a second presentation
            // means two parties hold it and only one of them should. There is
            // no way to tell which is asking, so neither keeps the session.
            refreshTokens.revokeFamily(stored.familyId, now)
            throw InvalidRefreshToken()
        }
        if (!stored.isUsable(now)) throw InvalidRefreshToken()

        val user = users.findById(stored.userId) ?: throw InvalidRefreshToken()
        val replacement = tokens.issueRefreshToken()
        refreshTokens.rotate(stored.id, stored.userId, stored.familyId, replacement, now)

        return Session(user, tokens.issueAccessToken(user.id), replacement)
    }

    /** Signs out the device holding [refreshToken]. Others keep their sessions. */
    fun logout(refreshToken: String) {
        refreshTokens.revokeByHash(Tokens.hash(refreshToken), tokens.now())
    }

    fun userById(id: UUID): User? = users.findById(id)

    private fun startSession(userId: UUID, user: User, now: java.time.Instant): Session {
        val refresh = tokens.issueRefreshToken()
        // A new sign-in starts its own family, so revoking it later cannot
        // sign out the phone that signed in yesterday.
        refreshTokens.store(userId, familyId = UUID.randomUUID(), token = refresh, now = now)
        return Session(user, tokens.issueAccessToken(userId), refresh)
    }

    private fun validateEmail(email: String): String {
        val clean = email.normaliseEmail()
        if (clean.isEmpty()) throw ValidationFailed("email", "Email is required")
        if (clean.length > MAX_EMAIL) {
            throw ValidationFailed("email", "Email must be $MAX_EMAIL characters or fewer")
        }
        // Deliberately loose. The only authority on whether an address exists is
        // whether mail to it arrives, and a strict regex here would reject valid
        // addresses to catch typos it cannot actually detect.
        if (!EMAIL_SHAPE.matches(clean)) throw ValidationFailed("email", "That does not look like an email address")
        return clean
    }

    private fun validatePassword(password: String) {
        if (password.length < Passwords.MIN_LENGTH) {
            throw ValidationFailed("password", "Password must be at least ${Passwords.MIN_LENGTH} characters")
        }
        if (password.length > Passwords.MAX_LENGTH) {
            throw ValidationFailed("password", "Password must be ${Passwords.MAX_LENGTH} characters or fewer")
        }
    }

    private companion object {
        const val MAX_EMAIL = 320
        const val MAX_DISPLAY_NAME = 80
        val EMAIL_SHAPE = Regex("""^[^@\s]+@[^@\s.]+(\.[^@\s.]+)+$""")

        /**
         * A real BCrypt hash of a value nobody knows, verified against when the
         * email is unknown purely to spend the same time a real check would.
         */
        const val DUMMY_HASH = "\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
    }
}
