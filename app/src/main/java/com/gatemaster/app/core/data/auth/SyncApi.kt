package com.gatemaster.app.core.data.auth

import android.util.Log
import com.gatemaster.protocol.AttemptPage
import com.gatemaster.protocol.ErrorResponse
import com.gatemaster.protocol.LoginRequest
import com.gatemaster.protocol.ProgressConflictResponse
import com.gatemaster.protocol.ProgressPutRequest
import com.gatemaster.protocol.ProgressResponse
import com.gatemaster.protocol.RefreshRequest
import com.gatemaster.protocol.RegisterRequest
import com.gatemaster.protocol.SessionResponse
import com.gatemaster.protocol.SyncedAttempt
import com.gatemaster.protocol.UploadAttemptsRequest
import com.gatemaster.protocol.UploadResult
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Why a call did not produce a result. */
sealed interface ApiError {
    /** No server URL is set for this build or install. Not a failure -- a state. */
    data object NotConfigured : ApiError

    /** The request never got an answer: no network, DNS, timeout, TLS. */
    data class Unreachable(val cause: Throwable) : ApiError

    /** The server answered, and said no. */
    data class Rejected(val status: Int, val body: ErrorResponse) : ApiError

    /** The session is gone: revoked, expired past refresh, or the account was deleted. */
    data object SignedOut : ApiError

    /** A progress write raced another device. Carries what the server holds. */
    data class Conflict(val current: ProgressResponse) : ApiError
}

sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Failed(val error: ApiError) : ApiResult<Nothing>
}

inline fun <T> ApiResult<T>.onOk(body: (T) -> Unit): ApiResult<T> =
    also { if (this is ApiResult.Ok) body(value) }

/**
 * The sync API, as the app sees it.
 *
 * Every method returns [ApiResult] rather than throwing. A sync failure is an
 * ordinary condition here -- the phone is on a train, the server is asleep on a
 * free tier -- and the app has to carry on regardless, so "no network" is
 * modelled as a value the caller must look at rather than an exception it can
 * forget to catch.
 */
class SyncApi(
    /** Suspending, because the URL is a per-install setting the user can change. */
    private val baseUrl: suspend () -> String?,
    private val tokens: SessionStore,
    engine: HttpClientEngine? = null,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client: HttpClient = build(engine)

    private fun build(engine: HttpClientEngine?): HttpClient {
        val configure: HttpClientConfig<*>.() -> Unit = {
            install(ContentNegotiation) { json(json) }

            install(HttpTimeout) {
                // Short on purpose. Sync is background work with nobody
                // watching, and a request that hangs for a minute holds a
                // wakelock for a minute.
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        tokens.current()?.let { BearerTokens(it.accessToken, it.refreshToken) }
                    }

                    // Attach the token up front rather than after a 401. Waiting
                    // for the challenge would double every authenticated request.
                    // Auth endpoints are excluded: /login must not carry the
                    // credentials of whoever was signed in before.
                    sendWithoutRequest { request ->
                        AUTH_SEGMENT !in request.url.pathSegments
                    }

                    refreshTokens {
                        val stored = tokens.current()
                        val base = baseUrl()
                        if (stored == null || base.isNullOrBlank()) return@refreshTokens null

                        // `client` here is the plugin's own, which does not
                        // re-enter this block -- otherwise a failing refresh
                        // would recurse until the stack ran out.
                        val response = runCatching {
                            client.post("${base.trimEnd('/')}/v1/auth/refresh") {
                                contentType(ContentType.Application.Json)
                                setBody(RefreshRequest(stored.refreshToken))
                                markAsRefreshTokenRequest()
                            }
                        }.getOrElse {
                            // Unreachable, not rejected. Keep the session: the
                            // user is offline, not signed out.
                            Log.w(TAG, "Token refresh could not reach the server", it)
                            return@refreshTokens null
                        }

                        if (!response.status.isSuccess()) {
                            // The server refused it. Either it expired past its
                            // two months or reuse detection revoked the family.
                            // Either way this device cannot recover without the
                            // password, so drop the session rather than retry
                            // forever.
                            Log.i(TAG, "Refresh refused (${response.status}); signing out")
                            tokens.clear()
                            return@refreshTokens null
                        }

                        val session: SessionResponse = response.body()
                        tokens.save(session.toStored())
                        BearerTokens(session.accessToken, session.refreshToken)
                    }
                }
            }
        }
        return if (engine != null) HttpClient(engine, configure) else HttpClient(OkHttp, configure)
    }

    // --- Auth ---------------------------------------------------------------

    suspend fun register(email: String, password: String, displayName: String): ApiResult<SessionResponse> =
        call { base ->
            client.post("$base/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(email, password, displayName))
            }
        }

    suspend fun login(email: String, password: String): ApiResult<SessionResponse> =
        call { base ->
            client.post("$base/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }
        }

    suspend fun logout(refreshToken: String): ApiResult<Unit> =
        callUnit { base ->
            client.post("$base/v1/auth/logout") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken))
            }
        }

    // --- Sync ---------------------------------------------------------------

    suspend fun progress(): ApiResult<ProgressResponse> =
        call { base -> client.get("$base/v1/sync/progress") }

    suspend fun putProgress(document: String, revision: Long): ApiResult<ProgressResponse> =
        call { base ->
            client.put("$base/v1/sync/progress") {
                contentType(ContentType.Application.Json)
                setBody(ProgressPutRequest(document, revision))
            }
        }

    suspend fun uploadAttempts(attempts: List<SyncedAttempt>): ApiResult<UploadResult> =
        call { base ->
            client.post("$base/v1/sync/attempts") {
                contentType(ContentType.Application.Json)
                setBody(UploadAttemptsRequest(attempts))
            }
        }

    suspend fun attemptsSince(since: Long, limit: Int = 50): ApiResult<AttemptPage> =
        call { base -> client.get("$base/v1/sync/attempts?since=$since&limit=$limit") }

    // --- Plumbing -----------------------------------------------------------

    private suspend inline fun <reified T> call(
        request: (base: String) -> HttpResponse,
    ): ApiResult<T> {
        val base = baseUrl()?.trimEnd('/')
        if (base.isNullOrBlank()) return ApiResult.Failed(ApiError.NotConfigured)

        val response = runCatching { request(base) }
            .getOrElse { return ApiResult.Failed(ApiError.Unreachable(it)) }

        return when {
            response.status.isSuccess() ->
                runCatching { ApiResult.Ok(response.body<T>()) }
                    .getOrElse { ApiResult.Failed(ApiError.Unreachable(it)) }

            response.status == HttpStatusCode.Conflict -> conflictOrRejected(response)

            // A 401 means two different things depending on where it came
            // from. On /auth/login it is a wrong password, and reporting it as
            // SignedOut would make the app clear a session that was never at
            // fault. Anywhere else it is the session itself.
            response.status == HttpStatusCode.Unauthorized ->
                if (response.wasAuthEndpoint()) {
                    ApiResult.Failed(ApiError.Rejected(response.status.value, response.errorBody()))
                } else {
                    ApiResult.Failed(ApiError.SignedOut)
                }

            else -> ApiResult.Failed(ApiError.Rejected(response.status.value, response.errorBody()))
        }
    }

    // Not inline, unlike `call` above, so the lambda has to be declared
    // suspend for the HTTP call inside it to be legal.
    private suspend fun callUnit(request: suspend (base: String) -> HttpResponse): ApiResult<Unit> {
        val base = baseUrl()?.trimEnd('/')
        if (base.isNullOrBlank()) return ApiResult.Failed(ApiError.NotConfigured)

        val response = runCatching { request(base) }
            .getOrElse { return ApiResult.Failed(ApiError.Unreachable(it)) }

        return if (response.status.isSuccess()) {
            ApiResult.Ok(Unit)
        } else {
            ApiResult.Failed(ApiError.Rejected(response.status.value, response.errorBody()))
        }
    }

    /**
     * A 409 is a progress conflict on the sync routes and an already-registered
     * email on the auth ones, so the body decides which.
     */
    private suspend fun <T> conflictOrRejected(response: HttpResponse): ApiResult<T> {
        val text = runCatching { response.bodyAsTextSafely() }.getOrDefault("")
        runCatching { json.decodeFromString<ProgressConflictResponse>(text) }
            .getOrNull()
            ?.let { return ApiResult.Failed(ApiError.Conflict(it.current)) }

        val error = runCatching { json.decodeFromString<ErrorResponse>(text) }
            .getOrDefault(ErrorResponse("conflict", "That did not go through"))
        return ApiResult.Failed(ApiError.Rejected(response.status.value, error))
    }

    private fun HttpResponse.wasAuthEndpoint(): Boolean =
        AUTH_SEGMENT in request.url.pathSegments

    private suspend fun HttpResponse.errorBody(): ErrorResponse = runCatching {
        json.decodeFromString<ErrorResponse>(bodyAsTextSafely())
    }.getOrDefault(ErrorResponse("http_${status.value}", "That did not go through"))

    private companion object {
        const val TAG = "SyncApi"

        /** The path segment shared by every unauthenticated endpoint. */
        const val AUTH_SEGMENT = "auth"
    }
}

/**
 * The body as text, or empty.
 *
 * Only ever used on a failure path, where the body is a bonus: a response that
 * cannot even be read as text should not turn a useful "the server said no"
 * into a crash.
 */
private suspend fun HttpResponse.bodyAsTextSafely(): String =
    runCatching { bodyAsText() }.getOrDefault("")

/** Wire session to the shape [TokenStore] keeps. */
fun SessionResponse.toStored() = StoredSession(
    accessToken = accessToken,
    accessTokenExpiresAt = accessTokenExpiresAt,
    refreshToken = refreshToken,
    userId = user.id,
    email = user.email,
    displayName = user.displayName,
)
