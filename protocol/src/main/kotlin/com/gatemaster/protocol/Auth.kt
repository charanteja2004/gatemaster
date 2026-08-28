package com.gatemaster.protocol

import kotlinx.serialization.Serializable

/**
 * The authentication half of the wire contract.
 *
 * These types are compiled into both the app and the server, so a renamed field
 * is a compile error on both sides at once rather than a 400 discovered by a
 * user. That is the entire reason this module exists.
 *
 * Two rules keep it able to stay that way:
 *
 * - **Every addition gets a default.** An old client must be able to parse a
 *   new server's response, and a new server must be able to parse an old
 *   client's request. A field without a default breaks one of those.
 * - **Nothing here may reference a platform type.** No java.time, no UUID, no
 *   Android. Times are epoch milliseconds and ids are strings, which are the
 *   only shapes JSON has anyway.
 */

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
data class UserResponse(
    val id: String,
    val email: String,
    val displayName: String,
)

/** What the client stores after any successful auth call. */
@Serializable
data class SessionResponse(
    val accessToken: String,
    /**
     * Sent so the client can refresh a minute early rather than discovering the
     * expiry as a 401 on a request the user was waiting for. Advisory: the
     * server verifies the token's own claim regardless.
     */
    val accessTokenExpiresAt: Long,
    val refreshToken: String,
    val user: UserResponse,
)

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
) {
    companion object {
        const val EMAIL_TAKEN = "email_taken"
        const val INVALID_CREDENTIALS = "invalid_credentials"
        const val INVALID_REFRESH_TOKEN = "invalid_refresh_token"
        const val VALIDATION_FAILED = "validation_failed"
        const val UNAUTHORIZED = "unauthorized"
    }
}
