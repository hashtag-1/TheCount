package com.sandesh.thecount.data.sync

import android.util.Log
import com.sandesh.thecount.data.remote.SupabaseProvider
import com.sandesh.thecount.data.remote.dto.PaymentRowDto
import com.sandesh.thecount.data.remote.dto.StrokeRowDto
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Shared Json that silently ignores server columns we don't model (created_at, user_id, etc.)
 * so adding new server columns never breaks the app.
 */
private val pullJson = Json { ignoreUnknownKeys = true }

/**
 * Fetches all stroke rows that belong to the signed-in user (RLS enforces ownership on the
 * server — no user_id filter needed here). Network errors are rethrown so SyncWorker retries.
 */
class StrokePuller {

    suspend fun pullAll(): List<StrokeRowDto> = withContext(Dispatchers.IO) {
        val result = try {
            SupabaseProvider.client.postgrest
                .from("daily_strokes")
                .select()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Network / HTTP failure — rethrow so the worker retries.
            throw e
        }

        try {
            pullJson.decodeFromString<List<StrokeRowDto>>(result.data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("TheCountSync", "Stroke download decoding failed", e)
            throw IllegalStateException("Failed to decode strokes from Supabase", e)
        }
    }
}

/**
 * Fetches all payment rows for the signed-in user.
 */
class PaymentPuller {

    suspend fun pullAll(): List<PaymentRowDto> = withContext(Dispatchers.IO) {
        val result = try {
            SupabaseProvider.client.postgrest
                .from("payments")
                .select()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e
        }

        try {
            pullJson.decodeFromString<List<PaymentRowDto>>(result.data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("TheCountSync", "Payment download decoding failed", e)
            throw IllegalStateException("Failed to decode payments from Supabase", e)
        }
    }
}
