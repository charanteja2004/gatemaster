package com.gatemaster.server.sync

import com.gatemaster.protocol.ProgressResponse

/**
 * The write was based on a revision the server has already moved past.
 *
 * Carries the current state so the client can merge without a second round
 * trip -- it needs the server's document to merge anyway, and it has just
 * proved it does not have it.
 *
 * The wire types this is built from live in `:protocol`, shared with the app.
 * This exception does not: it is how the server signals the rejection
 * internally, and the app learns about it as a 409.
 */
class ProgressConflict(val current: ProgressResponse) :
    Exception("Study progress has changed on another device")
