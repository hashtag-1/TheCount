package com.sandesh.thecount.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedSessionManager(private val context: Context) : SessionManager {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "TheCount_SessionKey"
        private const val PREF_NAME = "TheCount_EncryptedSessionPrefs"
        private const val SESSION_KEY = "supabase_session"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private val json = Json { ignoreUnknownKeys = true }
    }

    private val secretKey: SecretKey by lazy { getOrCreateKey() }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun prefs() = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override suspend fun saveSession(session: UserSession) {
        val jsonString = json.encodeToString(UserSession.serializer(), session)
        prefs().edit().putString(SESSION_KEY, encrypt(jsonString)).apply()
    }

    override suspend fun loadSession(): UserSession? {
        val stored = prefs().getString(SESSION_KEY, null) ?: return null
        return try {
            val jsonString = decrypt(stored)
            json.decodeFromString(UserSession.serializer(), jsonString)
        } catch (e: Exception) {
            // Old, corrupt or undecryptable value: forget it so the user simply signs in again.
            // Nothing sensitive is logged.
            prefs().edit().remove(SESSION_KEY).apply()
            null
        }
    }

    override suspend fun deleteSession() {
        prefs().edit().remove(SESSION_KEY).apply()
    }

    private fun encrypt(plaintext: String): String {
        // A new Cipher per call (Cipher objects are not thread-safe).
        // No IV is supplied: the Android Keystore generates it and we read it back from cipher.iv.
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

        // Stored value = IV bytes followed by ciphertext bytes, Base64 encoded.
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))

        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }
}