package com.sandesh.thecount.data.repository

import android.content.Context
import android.util.Log
import com.sandesh.thecount.BuildConfig
import com.sandesh.thecount.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

sealed class AuthResult {
    data object Success : AuthResult()
    data object Cancelled : AuthResult()
    data object NetworkError : AuthResult()
    data class NoGoogleAccount(val message: String = "No Google account is available on this phone. Add one in Settings and try again.") : AuthResult()
    data object AccessDenied : AuthResult()
    data class Unknown(val message: String = "Something went wrong. Please try again.") : AuthResult()
}

class AuthRepository {

    /**
     * [nonce] must be the RAW nonce. The screen sends the SHA-256 hash of it to Google
     * and passes the raw value here. Do not hash it in this function.
     */
    suspend fun signInWithGoogle(context: Context, idToken: String, nonce: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                SupabaseProvider.client.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    provider = Google
                    this.nonce = nonce
                }
                AuthResult.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Log only the exception name and the first line, never headers, keys or tokens.
                val firstLine = e.message?.lineSequence()?.firstOrNull().orEmpty()
                Log.e("TheCountAuth", "${e.javaClass.simpleName}: $firstLine")

                val message = e.message.orEmpty()
                when {
                    // Supabase rejected the account on purpose (invite rule)
                    message.contains("THECOUNT_", ignoreCase = true) ||
                            message.contains("Database error saving new user", ignoreCase = true) ->
                        AuthResult.AccessDenied

                    // Real network failures only, decided by exception type
                    e is IOException || e is HttpRequestException ->
                        AuthResult.NetworkError

                    else -> AuthResult.Unknown()
                }
            }
        }

    fun currentSession() = SupabaseProvider.client.auth.currentSessionOrNull()

    suspend fun signOut() = withContext(Dispatchers.IO) {
        SupabaseProvider.client.auth.signOut()
    }

    fun getGoogleWebClientId(): String = BuildConfig.GOOGLE_WEB_CLIENT_ID
}