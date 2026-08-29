package com.gatemaster.server.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.gatemaster.protocol.PasswordRules
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

/**
 * Password hashing.
 *
 * BCrypt, because it is deliberately slow and because its cost factor can be
 * raised later without invalidating existing hashes -- the cost is encoded in
 * the hash itself, so an old hash still verifies while new ones get harder.
 */
class Passwords(private val cost: Int) {

    fun hash(password: String): String =
        BCrypt.withDefaults().hashToString(cost, prepare(password))

    /**
     * Verifies [password] against [hash].
     *
     * Returns false rather than throwing on a malformed hash: a corrupted row
     * should read as "wrong password" to the caller, not as a 500 that tells an
     * attacker they found something interesting.
     */
    fun verify(password: String, hash: String): Boolean = runCatching {
        BCrypt.verifyer().verify(prepare(password), hash.toCharArray()).verified
    }.getOrDefault(false)

    /**
     * BCrypt reads at most 72 bytes of input and silently ignores the rest, so
     * a 200-character passphrase would be no stronger than its first 72 bytes,
     * and the library throws rather than truncating.
     *
     * Hashing to a fixed 44-character digest first removes the limit: every
     * character of the password reaches BCrypt, by way of SHA-256. This is the
     * same pre-hash Django and Dropbox use for exactly this reason. It is safe
     * here because the digest is never stored or used anywhere else -- it
     * exists only as BCrypt's input.
     */
    private fun prepare(password: String): CharArray {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest).toCharArray()
    }

    companion object {
        // From :protocol, so the app can show the same minimum in its form
        // rather than keeping a second copy that drifts from this one.
        const val MIN_LENGTH = PasswordRules.MIN_LENGTH
        const val MAX_LENGTH = PasswordRules.MAX_LENGTH
    }
}
