package com.sandesh.thecount.data.repository

import android.content.Context
import com.sandesh.thecount.BuildConfig
import com.sandesh.thecount.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

sealed class AuthResult {
    data object Success : AuthResult()
    data object Cancelled : AuthResult()
    data object NetworkError : AuthResult()
    data object AccessDenied : AuthResult()
    data object Unknown : AuthResult()
}

class AuthRepository {
    suspend fun signInWithGoogle(context: Context, idToken: String, nonce: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                SupabaseProvider.client.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    provider = Google
                    this.nonce = nonce
                }
                AuthResult.Success
            } catch (e: Exception) {
                val message = e.message.orEmpty()
                when {
                    message.contains("THECOUNT_NOT_INVITED", true) ||
                    message.contains("THECOUNT_LIMIT_REACHED", true) ||
                    message.contains("THECOUNT_DENIED", true) ||
                    message.contains("Database error saving new user", true) ||
                    message.contains("Access denied", true) ||
                    message.contains("not authorized", true) ||
                    message.contains("forbidden", true) -> AuthResult.AccessDenied
                    message.contains("network", true) ||
                    message.contains("timeout", true) ||
                    message.contains("connection", true) ||
                    message.contains("unreachable", true) ||
                    message.contains("IOException", true) ||
                    message.contains("UnknownHost", true) -> AuthResult.NetworkError
                    message.contains("cancelled", true) ||
                    message.contains("canceled", true) ||
                    message.contains("user cancelled", true) ||
                    message.contains("user canceled", true) -> AuthResult.Cancelled
                    else -> AuthResult.Unknown
                }
            }
        }

    fun currentSession() = SupabaseProvider.client.auth.currentSessionOrNull()

    suspend fun signOut() = withContext(Dispatchers.IO) {
        SupabaseProvider.client.auth.signOut()
    }

    fun generateNonce(): String {
        val uuid = UUID.randomUUID().toString()
        val bytes = uuid.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun getGoogleWebClientId(): String = BuildConfig.GOOGLE_WEB_CLIENT_ID
}