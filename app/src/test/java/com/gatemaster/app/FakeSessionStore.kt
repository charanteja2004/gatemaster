package com.gatemaster.app

import com.gatemaster.app.core.data.auth.SessionStore
import com.gatemaster.app.core.data.auth.StoredSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

/**
 * The session store, without the Android Keystore behind it.
 *
 * The real one encrypts through hardware that does not exist on the JVM. What
 * the tests below are about is the protocol -- when a token is sent, when it is
 * refreshed, when a session is dropped -- and none of that depends on how the
 * bytes are held at rest.
 */
class FakeSessionStore(initial: StoredSession? = null) : SessionStore {
    private val state = MutableStateFlow(initial)

    /** Every save, in order, so a test can assert a refresh actually persisted. */
    val saved = mutableListOf<StoredSession>()
    var cleared = 0
        private set

    override val session: Flow<StoredSession?> = state

    override suspend fun save(session: StoredSession) {
        saved += session
        state.value = session
    }

    override suspend fun clear() {
        cleared++
        state.value = null
    }

    override suspend fun current(): StoredSession? = session.first()
}

fun storedSession(
    accessToken: String = "access-1",
    refreshToken: String = "refresh-1",
    email: String = "student@example.com",
) = StoredSession(
    accessToken = accessToken,
    accessTokenExpiresAt = 0,
    refreshToken = refreshToken,
    userId = "11111111-1111-1111-1111-111111111111",
    email = email,
    displayName = "Student",
)
