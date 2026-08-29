package com.gatemaster.protocol

/**
 * The password rules, in the one place both sides can see them.
 *
 * The server is still the authority: it rejects what it rejects, and the app
 * shows whatever it says. But a form that only reveals the minimum length after
 * a round trip is a form that wastes the user's time, and a form that hard-codes
 * its own copy of the number is one that will eventually disagree with the
 * server about it.
 *
 * So the number lives here, in the contract, and the app uses it for a hint
 * while the server uses it for the actual decision.
 */
object PasswordRules {
    const val MIN_LENGTH = 8

    /**
     * Not a BCrypt limit -- the server pre-hashes, so length is unbounded there
     * -- but a cap on how much work an unauthenticated caller can ask for.
     */
    const val MAX_LENGTH = 200
}
