package com.gatemaster.server.api

import com.gatemaster.protocol.ErrorResponse
import com.gatemaster.protocol.LoginRequest
import com.gatemaster.protocol.ProgressPutRequest
import com.gatemaster.protocol.ProgressResponse
import com.gatemaster.protocol.RefreshRequest
import com.gatemaster.protocol.RegisterRequest
import com.gatemaster.protocol.UploadAttemptsRequest
import com.gatemaster.server.auth.AuthService
import com.gatemaster.server.sync.SyncRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.time.Instant
import java.util.UUID

const val JWT_AUTH = "jwt"

/** The id of the signed-in caller, from the verified access token. */
fun RoutingCall.userId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.subject!!)

fun Route.authRoutes(auth: AuthService) {
    route("/auth") {
        post("/register") {
            val body = call.receive<RegisterRequest>()
            val session = auth.register(body.email, body.password, body.displayName)
            call.respond(HttpStatusCode.Created, session.toResponse())
        }

        post("/login") {
            val body = call.receive<LoginRequest>()
            call.respond(auth.login(body.email, body.password).toResponse())
        }

        post("/refresh") {
            val body = call.receive<RefreshRequest>()
            call.respond(auth.refresh(body.refreshToken).toResponse())
        }

        post("/logout") {
            val body = call.receive<RefreshRequest>()
            auth.logout(body.refreshToken)
            // 204 whether or not the token was live. Reporting "that token was
            // already revoked" would tell an unauthenticated caller which
            // guessed tokens exist, and the user's intent is satisfied either
            // way: they are signed out.
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

fun Route.meRoute(auth: AuthService) {
    authenticate(JWT_AUTH) {
        get("/me") {
            val user = auth.userById(call.userId())
            if (user == null) {
                // The token verified but the account is gone -- deleted while a
                // valid access token was still in flight.
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized", "Sign in again"))
            } else {
                call.respond(user.toResponse())
            }
        }
    }
}

fun Route.syncRoutes(sync: SyncRepository, now: () -> Instant) {
    authenticate(JWT_AUTH) {
        route("/sync") {
            get("/progress") {
                val progress = sync.progressFor(call.userId())
                // Revision 0 and an empty document is what a user who has never
                // synced looks like, and it is exactly the state a first write
                // must be based on -- so this is a 200, not a 404.
                call.respond(
                    ProgressResponse(
                        document = progress?.document ?: "",
                        revision = progress?.revision ?: 0L,
                    ),
                )
            }

            put("/progress") {
                val body = call.receive<ProgressPutRequest>()
                if (body.document.length > MAX_PROGRESS_BYTES) {
                    call.respond(
                        HttpStatusCode.PayloadTooLarge,
                        ErrorResponse(
                            "document_too_large",
                            "Study progress must be under ${MAX_PROGRESS_BYTES / 1024} KB",
                            field = "document",
                        ),
                    )
                    return@put
                }
                val written = sync.writeProgress(call.userId(), body.document, body.revision, now())
                call.respond(ProgressResponse(written.document, written.revision))
            }

            post("/attempts") {
                val body = call.receive<UploadAttemptsRequest>()
                if (body.attempts.size > MAX_ATTEMPTS_PER_UPLOAD) {
                    call.respond(
                        HttpStatusCode.PayloadTooLarge,
                        ErrorResponse(
                            "too_many_attempts",
                            "Upload at most $MAX_ATTEMPTS_PER_UPLOAD attempts at a time",
                            field = "attempts",
                        ),
                    )
                    return@post
                }
                call.respond(sync.uploadAttempts(call.userId(), body.attempts, now()))
            }

            get("/attempts") {
                val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_PAGE)
                    .coerceIn(1, MAX_PAGE)
                call.respond(sync.attemptsSince(call.userId(), since, limit))
            }
        }
    }
}

/** A study-progress document past this is a bug on the device, not a big reader. */
private const val MAX_PROGRESS_BYTES = 512 * 1024
private const val MAX_ATTEMPTS_PER_UPLOAD = 100
private const val DEFAULT_PAGE = 50
private const val MAX_PAGE = 200
