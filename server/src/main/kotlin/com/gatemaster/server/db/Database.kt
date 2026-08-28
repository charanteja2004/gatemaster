package com.gatemaster.server.db

import com.gatemaster.server.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource

/**
 * The database, as thin a wrapper over JDBC as the code can get away with.
 *
 * There is no ORM here on purpose. The whole schema is five tables and every
 * query in the server is written out in full below the API layer, which is
 * fewer moving parts than a mapping layer would be and leaves the SQL -- the
 * part with the interesting decisions in it -- visible.
 */
class Database(private val dataSource: DataSource) : AutoCloseable {

    /**
     * Runs [body] in a transaction, committing on return and rolling back on
     * any throw. Nested calls are not supported and are not needed: every
     * write path in this server is one transaction deep.
     */
    fun <T> transaction(body: (Connection) -> T): T =
        dataSource.connection.use { connection ->
            val previous = connection.autoCommit
            connection.autoCommit = false
            try {
                val result = body(connection)
                connection.commit()
                result
            } catch (t: Throwable) {
                runCatching { connection.rollback() }
                throw t
            } finally {
                runCatching { connection.autoCommit = previous }
            }
        }

    /** A read that needs no transaction of its own. */
    fun <T> read(body: (Connection) -> T): T = dataSource.connection.use(body)

    override fun close() {
        (dataSource as? AutoCloseable)?.close()
    }

    companion object {
        fun connect(config: Config): Database {
            val hikari = HikariConfig().apply {
                jdbcUrl = config.databaseUrl
                config.databaseUser?.let { username = it }
                config.databasePassword?.let { password = it }
                // Small on purpose: this API is IO-light, and the free Postgres
                // tiers it is meant to run on cap connections in single digits.
                maximumPoolSize = 8
                isAutoCommit = true
                poolName = "gatemaster"
            }
            val dataSource = HikariDataSource(hikari)
            migrate(dataSource)
            return Database(dataSource)
        }

        /**
         * Applies the versioned SQL under `db/migration`. Runs on every boot:
         * an already-migrated database is a no-op, so a deploy never needs a
         * separate migration step that someone can forget to run.
         */
        fun migrate(dataSource: DataSource) {
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate()
        }
    }
}

/** Runs [body] over a prepared statement, closing it afterwards. */
fun <T> Connection.query(sql: String, body: PreparedStatement.() -> T): T =
    prepareStatement(sql).use(body)

/**
 * An INSERT that hands back a column the database generated.
 *
 * Naming the column rather than passing RETURN_GENERATED_KEYS matters: given
 * the flag, PostgreSQL returns every column of the new row and H2 returns only
 * the identity, so `getLong(1)` would mean different things on the two. Asking
 * for one column by name makes it mean the same thing on both.
 */
fun <T> Connection.queryReturning(
    sql: String,
    generatedColumn: String,
    body: PreparedStatement.() -> T,
): T = prepareStatement(sql, arrayOf(generatedColumn)).use(body)

/** Maps every row of a result set, closing it afterwards. */
fun <T> PreparedStatement.map(body: (ResultSet) -> T): List<T> =
    executeQuery().use { rows ->
        buildList { while (rows.next()) add(body(rows)) }
    }

/** Maps the first row, or null when there is none. */
fun <T> PreparedStatement.firstOrNull(body: (ResultSet) -> T): T? =
    executeQuery().use { rows -> if (rows.next()) body(rows) else null }
