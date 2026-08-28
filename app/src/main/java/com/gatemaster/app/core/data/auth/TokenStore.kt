package com.gatemaster.app.core.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.tokenStore: DataStore<Preferences> by preferencesDataStore(name = "auth_tokens")

/** What the app holds after signing in. */
@Serializable
data class StoredSession(
    val accessToken: String,
    val accessTokenExpiresAt: Long,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val displayName: String,
)

/**
 * Where the signed-in session is kept.
 *
 * An interface because the real one needs the Android Keystore, which does not
 * exist on the JVM -- so the HTTP layer and the repository above it depend on
 * this and stay testable without a device, which is the rule the rest of this
 * app already follows.
 */
interface SessionStore {
    val session: Flow<StoredSession?>
    suspend fun save(session: StoredSession)
    suspend fun clear()

    /** A one-shot read, for callers that want a value and not a stream. */
    suspend fun current(): StoredSession?
}

/**
 * The signed-in session, encrypted at rest.
 *
 * App-private storage is already unreadable to other apps, and on a device with
 * a lock screen it is encrypted by the platform. This adds a second lock whose
 * key lives in the Android Keystore -- hardware-backed where the device has a
 * secure element -- so the key never exists in this process's memory as bytes
 * and cannot be read out of a backup or an `adb run-as` dump of the data
 * directory.
 *
 * It is not a defence against a rooted device with the app unlocked; nothing
 * on the client is. It is a defence against the token surviving somewhere it
 * was not meant to, and it is cheap. The refresh token is revocable server-side
 * either way, which is the actual backstop.
 */
class TokenStore(private val context: Context) : SessionStore {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override val session: Flow<StoredSession?> = context.tokenStore.data.map { prefs ->
        prefs[KEY_SESSION]?.let(::decryptSession)
    }

    override suspend fun save(session: StoredSession) {
        val encrypted = encrypt(json.encodeToString(session))
        context.tokenStore.edit { it[KEY_SESSION] = encrypted }
    }

    override suspend fun clear() {
        context.tokenStore.edit { it.remove(KEY_SESSION) }
    }

    override suspend fun current(): StoredSession? = session.first()

    private fun decryptSession(stored: String): StoredSession? = runCatching {
        json.decodeFromString<StoredSession>(decrypt(stored))
    }.getOrElse {
        // The key is gone or the ciphertext will not open: the device was
        // restored to different hardware, or the user removed their lock
        // screen and the platform dropped the key. Treat it as signed out --
        // there is nothing to recover, and the alternative is an app that
        // crashes on launch and cannot be signed into again.
        Log.w(TAG, "Stored session could not be decrypted; signing out", it)
        null
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        // The IV is generated per encryption and is not secret, but it is
        // required to decrypt, so it travels with the ciphertext. Reusing one
        // with the same key is the way GCM fails catastrophically, which is
        // why this never stores or reuses it.
        return Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES),
        )
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }

    /**
     * The Keystore key, created on first use.
     *
     * Deliberately not user-authentication-bound: requiring the lock screen
     * would mean sync could not run in the background, which is the only place
     * it ever runs.
     */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val TAG = "TokenStore"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "gatemaster_session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val IV_BYTES = 12
        val KEY_SESSION = stringPreferencesKey("session")
    }
}
