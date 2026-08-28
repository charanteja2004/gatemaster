package com.gatemaster.server.api

import com.gatemaster.server.auth.Session
import com.gatemaster.server.auth.User
import com.gatemaster.server.sync.SyncedAttempt
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String = "",
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class UserResponse(val id: String, val email: String, val displayName: String) {
    companion object {
        fun of(user: User) = UserResponse(user.id.toString(), user.email, user.displayName)
    }
}

/**
 * What the client stores after any successful auth call.
 *
 * accessTokenExpiresAt is sent so the client can refresh a minute early rather
 * than discovering the expiry through a 401 on a request the user was waiting
 * for. It is advisory: the server verifies the token's own claim regardless.
 */
@Serializable
data class SessionResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Long,
    val refreshToken: String,
    val user: UserResponse,
) {
    companion object {
        fun of(session: Session) = SessionResponse(
            accessToken = session.accessToken.value,
            accessTokenExpiresAt = session.accessToken.expiresAt.toEpochMilli(),
            refreshToken = session.refreshToken.value,
            user = UserResponse.of(session.user),
        )
    }
}

/**
 * One error shape for the whole API.
 *
 * [code] is for the client to branch on and never changes; [message] is for a
 * human and may. [field] is set only when the failure was one input's fault, so
 * a form can mark it.
 */
@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val field: String? = null,
)

@Serializable
data class ProgressResponse(val document: String, val revision: Long)

@Serializable
data class ProgressPutRequest(val document: String, val revision: Long)

/**
 * A rejected progress write, carrying the server's current document.
 *
 * The client needs the server's copy to merge, and it has just proved it does
 * not have it, so sending it with the rejection saves a round trip that would
 * otherwise happen every single time.
 */
@Serializable
data class ProgressConflictResponse(
    val code: String = "progress_conflict",
    val message: String,
    val current: ProgressResponse,
)

@Serializable
data class UploadAttemptsRequest(val attempts: List<SyncedAttempt>)
