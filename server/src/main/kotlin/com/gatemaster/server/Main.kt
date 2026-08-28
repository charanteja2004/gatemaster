package com.gatemaster.server

import com.gatemaster.server.db.Database
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

fun main() {
    val log = LoggerFactory.getLogger("com.gatemaster.server.Main")

    // Configuration first, and eagerly: a deployment missing a variable should
    // fail here, in the logs, on the first second -- not on whichever request
    // happens to need it.
    val config = Config.fromEnv()

    if (config.databaseUrl.startsWith("jdbc:h2:")) {
        // Supported so a developer can run this with nothing installed, and
        // called out loudly because H2 accepts SQL that PostgreSQL does not.
        // Anything that works here still has to be checked against the real
        // thing before it means anything.
        log.warn("Running on H2. This is for local development only; deploy against PostgreSQL.")
    }

    val database = Database.connect(config)
    log.info("Migrations applied; listening on {}", config.port)

    val server = embeddedServer(Netty, port = config.port) {
        module(database, config)
    }

    // Close the pool on shutdown so in-flight transactions get to finish and
    // the platform's stop signal does not leave connections held server-side.
    Runtime.getRuntime().addShutdownHook(
        Thread {
            log.info("Shutting down")
            server.stop(gracePeriodMillis = 2_000, timeoutMillis = 10_000)
            database.close()
        },
    )

    server.start(wait = true)
}
