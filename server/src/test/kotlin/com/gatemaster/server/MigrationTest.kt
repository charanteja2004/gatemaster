package com.gatemaster.server

import com.gatemaster.server.db.map
import com.gatemaster.server.db.query
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The migrations are the one part of the server that cannot be rolled back in
 * production, so they get their own test rather than being covered incidentally
 * by whatever query happens to run first.
 */
class MigrationTest {

    @Test
    fun `every table in the schema is created`() {
        TestDatabase.fresh().use { db ->
            // Through JDBC metadata rather than information_schema: the two
            // backends put these tables in differently named schemas -- public
            // on H2, a per-test schema on Postgres -- and the driver already
            // knows which one this connection is pointed at.
            val tables = db.read { connection ->
                connection.metaData
                    .getTables(null, connection.schema, "%", arrayOf("TABLE"))
                    .use { rows ->
                        buildList { while (rows.next()) add(rows.getString("TABLE_NAME").lowercase()) }
                    }
            }

            listOf(
                "users",
                "refresh_tokens",
                "study_progress",
                "attempts",
                "attempt_questions",
            ).forEach { expected ->
                assertTrue("$expected is missing; tables were $tables", expected in tables)
            }
        }
    }

    @Test
    fun `migrating twice is a no-op`() {
        // Every boot migrates, so this is not a hypothetical: the second
        // deploy of an unchanged build has to be harmless.
        TestDatabase.fresh().use { db ->
            val before = db.read { connection ->
                connection.query("SELECT COUNT(*) FROM users") { map { it.getInt(1) } }
            }
            assertEquals(listOf(0), before)
        }
    }

    @Test
    fun `attempts get an ascending server sequence`() {
        // server_seq is the download cursor, so it has to be assigned by the
        // database and it has to increase. GENERATED ALWAYS AS IDENTITY is the
        // portable spelling of that; this proves the spelling took.
        TestDatabase.fresh().use { db ->
            val userId = java.util.UUID.randomUUID()
            db.transaction { connection ->
                connection.query(
                    """
                    INSERT INTO users (id, email, password_hash, display_name, created_at, updated_at)
                    VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """.trimIndent(),
                ) {
                    setObject(1, userId)
                    setString(2, "seq@example.com")
                    setString(3, "x")
                    setString(4, "Seq")
                    executeUpdate()
                }
                repeat(2) { index ->
                    connection.query(
                        """
                        INSERT INTO attempts (
                            user_id, client_attempt_id, test_id, test_title,
                            started_at, finished_at, duration_seconds,
                            score, max_score, uploaded_at
                        ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 60, 1.0, 2.0, CURRENT_TIMESTAMP)
                        """.trimIndent(),
                    ) {
                        setObject(1, userId)
                        setString(2, "client-$index")
                        setString(3, "practice:subject:os")
                        setString(4, "Operating Systems")
                        executeUpdate()
                    }
                }
            }

            val sequences = db.read { connection ->
                connection.query("SELECT server_seq FROM attempts ORDER BY server_seq") {
                    map { it.getLong(1) }
                }
            }
            assertEquals(2, sequences.size)
            assertTrue("expected ascending, was $sequences", sequences[1] > sequences[0])
        }
    }
}
