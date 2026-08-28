package com.gatemaster.server.api

import com.gatemaster.protocol.SessionResponse
import com.gatemaster.protocol.UserResponse
import com.gatemaster.server.auth.Session
import com.gatemaster.server.auth.User

/**
 * Domain to wire.
 *
 * The wire types live in `:protocol` and are shared with the app, so they carry
 * no server-side knowledge -- no companion that knows what a [Session] is, no
 * reference to a database row. These functions are that knowledge, kept on the
 * server side of the boundary where it belongs.
 */

fun User.toResponse() = UserResponse(
    id = id.toString(),
    email = email,
    displayName = displayName,
)

fun Session.toResponse() = SessionResponse(
    accessToken = accessToken.value,
    accessTokenExpiresAt = accessToken.expiresAt.toEpochMilli(),
    refreshToken = refreshToken.value,
    user = user.toResponse(),
)
