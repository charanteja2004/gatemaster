package com.gatemaster.server

import com.gatemaster.server.db.Database
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

/**
 * A migrated, empty database per test.
 *
 * By default that is H2 in PostgreSQL compatibility mode, held in memory: the
 * suite then runs on a laptop with no Docker and no database, which is the same
 * bargain the Android side made by keeping every test on the JVM.
 *
 * H2 is not Postgres, though, and that gap is where a portable-SQL claim goes
 * to die -- jsonb, citext, ON CONFLICT and advisory locks would all pass here
 * and fail in production. So the same suite runs a second time against real
 * PostgreSQL whenever [POSTGRES_URL_ENV] is set, which CI always does. The
 * schema and the queries stay inside the portable subset because both runs have
 * to be green, not because a comment says so.
 */
object TestDatabase {
    /** Set this to run the whole suite against PostgreSQL instead of H2. */
    const val POSTGRES_URL_ENV = "GATEMASTER_TEST_POSTGRES_URL"

    private val counter = AtomicInteger()

    val isPostgres: Boolean get() = !System.getenv(POSTGRES_URL_ENV).isNullOrBlank()

    fun fresh(): Database {
        val name = "gatemaster_test_${counter.incrementAndGet()}"
        val dataSource = System.getenv(POSTGRES_URL_ENV)
            ?.takeIf { it.isNotBlank() }
            ?.let { postgres(it, name) }
            ?: h2(name)
        return Database(dataSource)
    }

    private fun h2(name: String): DataSource = JdbcDataSource().apply {
        // DB_CLOSE_DELAY=-1 keeps the database alive for the length of the JVM,
        // so the pool can reconnect rather than finding it gone between calls.
        setURL("jdbc:h2:mem:$name;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
        user = "sa"
        password = ""
    }.also { Database.migrate(it) }

    /**
     * One schema per test inside a shared database.
     *
     * Not one database per test: creating a Postgres database takes a
     * connection to a different one and about a second, and the suite has
     * thirty-odd tests. A schema is free, and `currentSchema` makes every
     * unqualified query in the server land inside it -- so the fixed email
     * addresses these tests use cannot collide across tests.
     */
    private fun postgres(baseUrl: String, schema: String): DataSource {
        val separator = if ('?' in baseUrl) "&" else "?"
        val dataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "$baseUrl${separator}currentSchema=$schema"
                maximumPoolSize = 4
                poolName = schema
            },
        )
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .schemas(schema)
            .defaultSchema(schema)
            // The schema does not exist yet; this is what creates it.
            .createSchemas(true)
            .load()
            .migrate()
        return dataSource
    }

    /** Config with everything the server needs, pointed at nothing in particular. */
    fun config(): Config = Config(
        port = 0,
        databaseUrl = "unused",
        databaseUser = null,
        databasePassword = null,
        jwtSecret = "test-secret-that-is-long-enough-to-pass-validation",
        jwtIssuer = "gatemaster-test",
        jwtAudience = "gatemaster-test-app",
        accessTokenLifetime = Duration.ofMinutes(15),
        refreshTokenLifetime = Duration.ofDays(60),
        // The floor, not the default: every test that registers a user pays
        // this cost, and at 12 the suite would spend its life hashing.
        bcryptCost = Config.MIN_BCRYPT_COST,
        allowedOrigins = emptyList(),
    )
}
