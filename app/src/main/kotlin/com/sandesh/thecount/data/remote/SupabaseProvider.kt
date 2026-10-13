package com.sandesh.thecount.data.remote

import android.app.Application
import com.sandesh.thecount.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

object SupabaseProvider {
    @Volatile
    private var _client: SupabaseClient? = null

    fun initialize(application: Application) {
        if (_client == null) {
            _client = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth) {
                    sessionManager = EncryptedSessionManager(application)
                }
                install(Postgrest)
            }
        }
    }

    val client: SupabaseClient
        get() = _client ?: throw IllegalStateException("SupabaseProvider not initialized. Call initialize(Application) first.")
}