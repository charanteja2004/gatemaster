package com.gatemaster.server

import com.gatemaster.server.api.ErrorResponse
import com.gatemaster.server.api.JWT_AUTH
import com.gatemaster.server.api.ProgressConflictResponse
import com.gatemaster.server.api.ProgressResponse
import com.gatemaster.server.api.authRoutes
import com.gatemaster.server.api.meRoute
import com.gatemaster.server.api.syncRoutes
import com.gatemaster.server.auth.AuthService
import com.gatemaster.server.auth.EmailAlreadyRegistered
import com.gatemaster.server.auth.InvalidCredentials
import com.gatemaster.server.auth.InvalidRefreshToken
import com.gatemaster.server.auth.Passwords
import com.gatemaster.server.auth.RefreshTokenRepository
import com.gatemaster.server.auth.Tokens
import com.gatemaster.server.auth.UserRepository
import com.gatemaster.server.auth.ValidationFailed
import com.gatemaster.server.db.Database
import com.gatemaster.server.sync.ProgressConflict
import com.gatemaster.server.sync.SyncRepository
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/** Rate-limited bucket for the endpoints an unauthenticated caller can reach. */
val AUTH_RATE_LIMIT = RateLimitName("auth")

/**
 * Wires the server.
 *
 * Dependencies are constructed here and passed down rather than resolved from a
 * container: there are six of them, and a graph that fits on one screen is
 * easier to follow than the framework that would hide it. This is the same call
 * the app made with its AppContainer.
 */
fun Application.module(
    database: Database,
    config: Config,
    clock: Clock = Clock.systemUTC(),
) {
    val tokens = Tokens(config, clock)
    val auth = AuthService(
        users = UserRepository(database),
        refreshTokens = RefreshTokenRepository(database),
        passwords = Passwords(config.bcryptCost),
        tokens = tokens,
    )
    val sync = SyncRepository(database)

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                // A client that is a version ahead may send fields this build
                // has never heard of; refusing the whole request over one would
                // make every server deploy a forced app update.
                encodeDefaults = true
            },
        )
    }

    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
        header("Referrer-Policy", "no-referrer")
    }

    install(CallLogging) {
        // Never log the path's query string or any body: the auth endpoints
        // take passwords and refresh tokens, and logs outlive requests.
        format { call -> "${call.request.local.method.value} ${call.request.path()} -> ${call.response.status()?.value}" }
    }

    if (config.allowedOrigins.isNotEmpty()) {
        install(CORS) {
            config.allowedOrigins.forEach { allowHost(it, schemes = listOf("http", "https")) }
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            allowMethod(io.ktor.http.HttpMethod.Put)
            allowMethod(io.ktor.http.HttpMethod.Post)
        }
    }

    install(RateLimit) {
        register(AUTH_RATE_LIMIT) {
            // Enough for a person mistyping a password, far short of what a
            // credential-stuffing run needs. Keyed on the caller's address.
            rateLimiter(limit = 20, refillPeriod = 1.minutes)
            requestKey { call -> call.request.local.remoteAddress }
        }
    }

    install(Authentication) {
        jwt(JWT_AUTH) {
            realm = "gatemaster"
            verifier(tokens.verifier)
            validate { credential ->
                // A token whose subject is not a UUID was not minted here, or
                // was minted by a version that meant something else by it.
                credential.subject
                    ?.let { runCatching { java.util.UUID.fromString(it) }.getOrNull() }
                    ?.let { JWTPrincipal(credential.payload) }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("unauthorized", "Sign in again"),
                )
            }
        }
    }

    installErrorHandling()

    routing {
        // Unversioned and unauthenticated, because it is what the platform's
        // health check calls, and that must not depend on the database being up
        // any more than it already does.
        get("/health") { call.respond(mapOf("status" to "ok")) }

        route("/v1") {
            rateLimit(AUTH_RATE_LIMIT) { authRoutes(auth) }
            meRoute(auth)
            syncRoutes(sync) { Instant.now(clock) }
        }
    }
}

/**
 * One place that turns a thrown domain type into a response.
 *
 * Routes therefore contain no error branches: they either produce a result or
 * throw the reason they could not, which keeps the happy path readable.
 */
private fun Application.installErrorHandling() {
    install(StatusPages) {
        exception<ValidationFailed> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("validation_failed", cause.message, cause.field),
            )
        }
        exception<EmailAlreadyRegistered> { call, _ ->
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse("email_taken", "That email is already registered", "email"),
            )
        }
        exception<InvalidCredentials> { call, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse("invalid_credentials", "Email or password is incorrect"),
            )
        }
        exception<InvalidRefreshToken> { call, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse("invalid_refresh_token", "Sign in again"),
            )
        }
        exception<ProgressConflict> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ProgressConflictResponse(
                    message = "Study progress changed on another device; merge and retry",
                    current = ProgressResponse(cause.current.document, cause.current.revision),
                ),
            )
        }
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("malformed_request", "That request body could not be read"),
            )
        }
        exception<Throwable> { call, cause ->
            // Logged in full, reported as nothing. An exception message can
            // carry a SQL fragment or a column name, and the caller has no use
            // for either.
            call.application.log.error("Unhandled failure on ${call.request.local.uri}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("internal_error", "Something went wrong. Try again."),
            )
        }
    }
}
