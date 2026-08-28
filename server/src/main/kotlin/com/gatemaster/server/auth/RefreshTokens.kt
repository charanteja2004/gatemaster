package com.gatemaster.server.auth

import com.gatemaster.server.db.Database
import com.gatemaster.server.db.firstOrNull
import com.gatemaster.server.db.query
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

data class StoredRefreshToken(
    val id: UUID,
    val userId: UUID,
    val familyId: UUID,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val revokedAt: Instant?,
) {
    fun isUsable(now: Instant): Boolean = revokedAt == null && expiresAt.isAfter(now)
}

class RefreshTokenRepository(private val database: Database) {

    /** Stores a token issued for a brand-new sign-in: a new family of one. */
    fun store(
        userId: UUID,
        familyId: UUID,
        token: RefreshTokenValue,
        now: Instant,
    ): UUID = database.transaction { connection ->
        val id = UUID.randomUUID()
        connection.query(
            """
            INSERT INTO refresh_tokens (id, user_id, family_id, token_hash, issued_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ) {
            setObject(1, id)
            setObject(2, userId)
            setObject(3, familyId)
            setString(4, token.hash)
            setTimestamp(5, Timestamp.from(now))
            setTimestamp(6, Timestamp.from(token.expiresAt))
            executeUpdate()
        }
        id
    }

    fun findByHash(hash: String): StoredRefreshToken? = database.read { connection ->
        connection.query(
            """
            SELECT id, user_id, family_id, issued_at, expires_at, revoked_at
            FROM refresh_tokens WHERE token_hash = ?
            """.trimIndent(),
        ) {
            setString(1, hash)
            firstOrNull { row ->
                StoredRefreshToken(
                    id = row.getObject("id", UUID::class.java),
                    userId = row.getObject("user_id", UUID::class.java),
                    familyId = row.getObject("family_id", UUID::class.java),
                    issuedAt = row.getTimestamp("issued_at").toInstant(),
                    expiresAt = row.getTimestamp("expires_at").toInstant(),
                    revokedAt = row.getTimestamp("revoked_at")?.toInstant(),
                )
            }
        }
    }

    /**
     * Revokes [previousId] and stores [replacement] in the same family, in one
     * transaction.
     *
     * Both halves have to land together. If the revoke committed alone the user
     * would be signed out by a successful refresh; if the insert committed
     * alone the old token would stay valid forever, which is the exact thing
     * rotation exists to prevent.
     */
    fun rotate(
        previousId: UUID,
        userId: UUID,
        familyId: UUID,
        replacement: RefreshTokenValue,
        now: Instant,
    ): UUID = database.transaction { connection ->
        connection.query(
            "UPDATE refresh_tokens SET revoked_at = ? WHERE id = ? AND revoked_at IS NULL",
        ) {
            setTimestamp(1, Timestamp.from(now))
            setObject(2, previousId)
            executeUpdate()
        }
        val id = UUID.randomUUID()
        connection.query(
            """
            INSERT INTO refresh_tokens (id, user_id, family_id, token_hash, issued_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ) {
            setObject(1, id)
            setObject(2, userId)
            setObject(3, familyId)
            setString(4, replacement.hash)
            setTimestamp(5, Timestamp.from(now))
            setTimestamp(6, Timestamp.from(replacement.expiresAt))
            executeUpdate()
        }
        id
    }

    /**
     * Revokes every live token descended from one sign-in.
     *
     * Called when an already-revoked token is presented. Rotation means each
     * token is usable exactly once, so a second use says the token was copied
     * -- and there is no way to tell from here whether the legitimate holder or
     * the thief is the one asking. Revoking the family signs both out, which
     * costs the real user one sign-in and costs the thief everything.
     */
    fun revokeFamily(familyId: UUID, now: Instant): Int = database.transaction { connection ->
        connection.query(
            "UPDATE refresh_tokens SET revoked_at = ? WHERE family_id = ? AND revoked_at IS NULL",
        ) {
            setTimestamp(1, Timestamp.from(now))
            setObject(2, familyId)
            executeUpdate()
        }
    }

    /** Signs one device out. Unknown or already-revoked hashes are a no-op. */
    fun revokeByHash(hash: String, now: Instant): Boolean = database.transaction { connection ->
        connection.query(
            "UPDATE refresh_tokens SET revoked_at = ? WHERE token_hash = ? AND revoked_at IS NULL",
        ) {
            setTimestamp(1, Timestamp.from(now))
            setString(2, hash)
            executeUpdate() > 0
        }
    }

    /**
     * Deletes tokens that expired before [before].
     *
     * Revoked-but-unexpired rows are kept deliberately: they are what reuse
     * detection matches against, and deleting them early would turn a stolen
     * token from "detected" into "unknown token", which is a quieter failure.
     */
    fun deleteExpired(before: Instant): Int = database.transaction { connection ->
        connection.query("DELETE FROM refresh_tokens WHERE expires_at < ?") {
            setTimestamp(1, Timestamp.from(before))
            executeUpdate()
        }
    }
}
