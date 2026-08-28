package com.gatemaster.server.auth

import com.gatemaster.server.db.Database
import com.gatemaster.server.db.firstOrNull
import com.gatemaster.server.db.query
import java.sql.SQLException
import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val displayName: String,
    val createdAt: Instant,
)

/** Thrown when an email is already registered. */
class EmailAlreadyRegistered : Exception("That email is already registered")

class UserRepository(private val database: Database) {

    /**
     * Inserts a user, or throws [EmailAlreadyRegistered].
     *
     * The duplicate is caught from the constraint rather than checked for
     * first. A SELECT-then-INSERT would be a race: two registrations for the
     * same address, arriving together, would both find nothing and both
     * insert. The unique index is the only thing that can actually decide.
     */
    fun create(email: String, passwordHash: String, displayName: String, now: Instant): User {
        val user = User(
            id = UUID.randomUUID(),
            email = email.normaliseEmail(),
            displayName = displayName.trim(),
            createdAt = now,
        )
        try {
            database.transaction { connection ->
                connection.query(
                    """
                    INSERT INTO users (id, email, password_hash, display_name, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                ) {
                    setObject(1, user.id)
                    setString(2, user.email)
                    setString(3, passwordHash)
                    setString(4, user.displayName)
                    setTimestamp(5, java.sql.Timestamp.from(now))
                    setTimestamp(6, java.sql.Timestamp.from(now))
                    executeUpdate()
                }
            }
        } catch (e: SQLException) {
            if (e.isUniqueViolation()) throw EmailAlreadyRegistered() else throw e
        }
        return user
    }

    /** The stored hash alongside the user, for the login path only. */
    fun findByEmailWithHash(email: String): Pair<User, String>? = database.read { connection ->
        connection.query(
            """
            SELECT id, email, display_name, created_at, password_hash
            FROM users WHERE email = ?
            """.trimIndent(),
        ) {
            setString(1, email.normaliseEmail())
            firstOrNull { row ->
                row.toUser() to row.getString("password_hash")
            }
        }
    }

    fun findById(id: UUID): User? = database.read { connection ->
        connection.query(
            "SELECT id, email, display_name, created_at FROM users WHERE id = ?",
        ) {
            setObject(1, id)
            firstOrNull { it.toUser() }
        }
    }
}

/**
 * Lower-cased and trimmed, because `Charan@Example.com` and
 * `charan@example.com` are the same mailbox and a user who registers with one
 * will sign in with the other. The unique index enforces this only because
 * every write and every lookup passes through here first.
 */
fun String.normaliseEmail(): String = trim().lowercase()

/**
 * SQLSTATE 23505 is `unique_violation` in the standard, and both PostgreSQL and
 * H2 report it. Matching on the code rather than the message keeps this working
 * when the message is localised or the constraint is renamed.
 */
fun SQLException.isUniqueViolation(): Boolean =
    generateSequence(this) { it.nextException ?: (it.cause as? SQLException) }
        .any { it.sqlState == "23505" }

private fun java.sql.ResultSet.toUser() = User(
    id = getObject("id", UUID::class.java),
    email = getString("email"),
    displayName = getString("display_name"),
    createdAt = getTimestamp("created_at").toInstant(),
)
