package com.sandesh.thecount.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.sandesh.thecount.BuildConfig
import com.sandesh.thecount.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.MessageDigest
import java.util.UUID

sealed class AuthResult {
    data object Success : AuthResult()
    data object Cancelled : AuthResult()
    data object NetworkError : AuthResult()
    data class NoGoogleAccount(val message: String = "No Google account is available on this phone. Add one in Settings and try again.") : AuthResult()
    data object AccessDenied : AuthResult()
    data class Unknown(val message: String = "Something went wrong. Please try again.") : AuthResult()
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
                Log.e("TheCountAuth", "${e.javaClass.simpleName}: ${e.message}")
                val message = e.message.orEmpty()

                // Network errors
                if (e is IOException ||
                    e is UnknownHostException ||
                    e is SocketTimeoutException ||
                    e is ConnectException ||
                    message.contains("timeout", true) ||
                    message.contains("timed out", true) ||
                    message.contains("connect", true) ||
                    message.contains("connection", true) ||
                    message.contains("network", true) ||
                    message.contains("unreachable", true) ||
                    message.contains("UnknownHost", true) ||
                    message.contains("SocketTimeout", true) ||
                    message.contains("ConnectException", true) ||
                    message.contains("HttpRequestTimeout", true) ||
                    message.contains("request timed out", true)
                ) {
                    return@withContext AuthResult.NetworkError
                }

                // Supabase rejections
                if (message.contains("THECOUNT_", true) ||
                    message.contains("Database error saving new user", true)
                ) {
                    return@withContext AuthResult.AccessDenied
                }

                return@withContext AuthResult.Unknown()
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