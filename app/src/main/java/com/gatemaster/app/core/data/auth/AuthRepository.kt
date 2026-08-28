package com.gatemaster.app.core.data.auth

import com.gatemaster.protocol.ErrorResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Who is signed in, if anyone. */
sealed interface AuthState {
    /** No server configured for this build or install. Sync is not on offer. */
    data object Unavailable : AuthState

    /** A server exists; nobody is signed in. The app works exactly as before. */
    data object SignedOut : AuthState

    data class SignedIn(
        val userId: String,
        val email: String,
        val displayName: String,
    ) : AuthState
}

/** What went wrong signing in, in terms the screen can show. */
data class AuthFailure(
    val message: String,
    /** "email" or "password" when one field is at fault, so the form can mark it. */
    val field: String? = null,
)

/**
 * Signing in and out.
 *
 * The app is usable signed out and always has been; this adds an optional
 * account, and every failure here has to leave studying working. That is why
 * nothing in this class can throw at its caller and why signing out succeeds
 * locally even when the server cannot be reached.
 */
class AuthRepository(
    private val api: SyncApi,
    private val tokens: SessionStore,
    private val serverConfigured: Flow<Boolean>,
) {

    val state: Flow<AuthState> = combineState()

    private fun combineState(): Flow<AuthState> =
        kotlinx.coroutines.flow.combine(tokens.session, serverConfigured) { session, configured ->
            when {
                session != null -> AuthState.SignedIn(
                    userId = session.userId,
                    email = session.email,
                    displayName = session.displayName,
                )
                // Deliberately checked second. A session that outlived the URL
                // being cleared still reads as signed in, so clearing the
                // server field in Settings does not silently discard an
                // account the user can still use once they put it back.
                !configured -> AuthState.Unavailable
                else -> AuthState.SignedOut
            }
        }

    /** True once there is a session, whatever the server setting says. */
    val isSignedIn: Flow<Boolean> = tokens.session.map { it != null }

    suspend fun register(email: String, password: String, displayName: String): AuthFailure? =
        when (val result = api.register(email.trim(), password, displayName.trim())) {
            is ApiResult.Ok -> {
                tokens.save(result.value.toStored())
                null
            }
            is ApiResult.Failed -> result.error.toFailure()
        }

    suspend fun login(email: String, password: String): AuthFailure? =
        when (val result = api.login(email.trim(), password)) {
            is ApiResult.Ok -> {
                tokens.save(result.value.toStored())
                null
            }
            is ApiResult.Failed -> result.error.toFailure()
        }

    /**
     * Signs out.
     *
     * The local session is cleared whatever the server says. Telling the server
     * is a courtesy that revokes the refresh token early; if the phone is
     * offline, the token expires on its own and the user is still signed out
     * here, which is what they asked for.
     */
    suspend fun logout() {
        tokens.current()?.let { api.logout(it.refreshToken) }
        tokens.clear()
    }

    private fun ApiError.toFailure(): AuthFailure = when (this) {
        ApiError.NotConfigured -> AuthFailure(
            "No sync server is set for this app. Add one in Settings.",
        )
        is ApiError.Unreachable -> AuthFailure(
            "Could not reach the server. Check your connection and try again.",
        )
        ApiError.SignedOut -> AuthFailure("Email or password is incorrect.", field = "password")
        is ApiError.Conflict -> AuthFailure("That did not go through. Try again.")
        is ApiError.Rejected -> when (body.code) {
            ErrorResponse.EMAIL_TAKEN ->
                AuthFailure("That email already has an account. Sign in instead.", field = "email")
            ErrorResponse.INVALID_CREDENTIALS ->
                AuthFailure("Email or password is incorrect.", field = "password")
            ErrorResponse.VALIDATION_FAILED -> AuthFailure(body.message, field = body.field)
            else -> AuthFailure(body.message.ifBlank { "That did not go through. Try again." })
        }
    }
}
