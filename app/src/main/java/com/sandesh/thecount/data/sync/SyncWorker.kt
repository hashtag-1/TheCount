package com.sandesh.thecount.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sandesh.thecount.data.db.TheCountDatabase
import com.sandesh.thecount.data.remote.SupabaseProvider
import com.sandesh.thecount.data.repository.SessionUserProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Background worker that pushes pending local rows (7A) then pulls server rows (7B).
 *
 * Key fix: we read the userId directly from the Supabase session instead of
 * SessionUserProvider, because the provider's internal coroutine may not have
 * resolved yet when the worker starts, causing it to see "anonymous" even when
 * the user is signed in.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TheCountSync"
        const val UNIQUE_WORK_NAME = "thecount-sync-push"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Read the session directly — this is always current, no coroutine race.
        val userId = SupabaseProvider.client.auth.currentSessionOrNull()?.user?.id
        if (userId == null) {
            Log.d(TAG, "SyncWorker: no active session, skipping")
            return@withContext Result.success()
        }

        Log.d(TAG, "SyncWorker: running for user $userId")

        val db = TheCountDatabase.getInstance(applicationContext)

        // Wrap the real userId in a simple provider so SyncManager and PullManager
        // don't need their own Supabase dependency.
        val userProvider = object : com.sandesh.thecount.data.repository.CurrentUserProvider {
            override val currentUserId: String = userId
            override val currentUserIdFlow =
                kotlinx.coroutines.flow.MutableStateFlow(userId).also { }
            override fun wasPromptedForAccount(userId: String) = false
            override fun markAccountPrompted(userId: String) {}
            override fun clearPromptedAccount() {}
        }

        // ── 7A: Push ─────────────────────────────────────────────────────────
        val pushManager = SyncManager(
            db = db,
            userProvider = userProvider,
            strokePusher = StrokePusher(),
            paymentPusher = PaymentPusher()
        )

        val pushed = try {
            pushManager.pushPending()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "SyncWorker: push network error, will retry: ${e.javaClass.simpleName}")
            return@withContext Result.retry()
        } catch (e: Exception) {
            if (e is io.github.jan.supabase.exceptions.HttpRequestException) {
                Log.w(TAG, "SyncWorker: push HTTP error, will retry: ${e.javaClass.simpleName}")
                return@withContext Result.retry()
            }
            Log.e(TAG, "SyncWorker: push error: ${e.javaClass.simpleName}: ${e.message}")
            return@withContext Result.failure()
        }

        Log.d(TAG, "SyncWorker: pushed $pushed rows")

        // ── 7B: Pull ─────────────────────────────────────────────────────────
        val pullManager = PullManager(
            db = db,
            userProvider = userProvider
        )

        val pulled = try {
            pullManager.pullAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "SyncWorker: pull network error, will retry: ${e.javaClass.simpleName}")
            return@withContext Result.retry()
        } catch (e: Exception) {
            if (e is io.github.jan.supabase.exceptions.HttpRequestException) {
                Log.w(TAG, "SyncWorker: pull HTTP error, will retry: ${e.javaClass.simpleName}")
                return@withContext Result.retry()
            }
            Log.e(TAG, "SyncWorker: pull error (non-fatal): ${e.javaClass.simpleName}: ${e.message}")
            return@withContext Result.success()
        }

        Log.d(TAG, "SyncWorker: pulled $pulled rows")
        Result.success()
    }
}
