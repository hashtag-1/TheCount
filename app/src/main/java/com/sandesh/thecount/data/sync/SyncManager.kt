package com.sandesh.thecount.data.sync

import android.util.Log
import com.sandesh.thecount.data.db.TheCountDatabase
import com.sandesh.thecount.data.repository.CurrentUserProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pushes every pending local row to the server. Called by [SyncWorker] on a background
 * thread with network access. Pull is a separate task (7B) - this class never writes to
 * the server except through the two push functions.
 *
 * Rules:
 * - Rows owned by "local" or "anonymous" are never pushed.
 * - Delete vs upsert is decided by deletedAt != null, not by sync_state.
 * - Network errors are rethrown so the worker retries; permanent errors mark the row ERROR.
 * - On Ok the server-assigned version is stored and the row becomes SYNCED.
 */
class SyncManager(
    private val db: TheCountDatabase,
    private val userProvider: CurrentUserProvider,
    private val strokePusher: StrokePusher = StrokePusher(),
    private val paymentPusher: PaymentPusher = PaymentPusher()
) {

    companion object {
        private const val TAG = "TheCountSync"
        private val DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE
    }

    /**
     * Pushes all pending rows for the current user. Returns the number of rows that were
     * successfully synced. Throws on network failure so the caller can retry.
     */
    suspend fun pushPending(): Int = coroutineScope {
        val userId = userProvider.currentUserId
        if (userId == "local" || userId == "anonymous") {
            Log.d(TAG, "SyncManager: no signed-in user, nothing to push")
            return@coroutineScope 0
        }

        val strokes = db.dailyStrokeDao().getPending(userId)
        val payments = db.paymentDao().getPending(userId)
        val total = strokes.size + payments.size
        if (total == 0) {
            Log.d(TAG, "SyncManager: no pending rows")
            return@coroutineScope 0
        }

        var synced = 0
        var hadPermanentError = false

        for (entity in strokes) {
            val result = try {
                strokePusher.push(
                    id = entity.id.toString(),
                    workDate = entity.workDate.format(DATE_FORMAT),
                    strokeCount = entity.strokeCount,
                    deleted = entity.deletedAt != null,
                    baseVersion = entity.serverVersion
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Network / transport failure - rethrow so the worker retries.
                throw e
            }

            when (result) {
                is SyncPushResult.Ok -> {
                    db.dailyStrokeDao().markSynced(userId, entity.id, result.version)
                    synced++
                }
                is SyncPushResult.Conflict -> {
                    db.dailyStrokeDao().markConflict(userId, entity.id)
                    Log.w(TAG, "SyncManager: stroke ${entity.id} conflict: ${result.reason}")
                }
                is SyncPushResult.PermanentError -> {
                    db.dailyStrokeDao().markError(userId, entity.id)
                    hadPermanentError = true
                    Log.w(TAG, "SyncManager: stroke ${entity.id} permanent error: ${result.message}")
                }
                is SyncPushResult.Malformed -> {
                    db.dailyStrokeDao().markError(userId, entity.id)
                    hadPermanentError = true
                    Log.w(TAG, "SyncManager: stroke ${entity.id} malformed: ${result.detail}")
                }
                is SyncPushResult.NetworkError -> {
                    // Should not happen here - the pusher rethrows transport errors.
                    throw IllegalStateException("unexpected NetworkError from stroke pusher")
                }
            }
        }

        for (entity in payments) {
            val result = try {
                paymentPusher.push(
                    id = entity.id.toString(),
                    paymentDate = entity.paymentDate.format(DATE_FORMAT),
                    amount = entity.amount,
                    note = entity.note,
                    deleted = entity.deletedAt != null,
                    baseVersion = entity.serverVersion
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw e
            }

            when (result) {
                is SyncPushResult.Ok -> {
                    db.paymentDao().markSynced(userId, entity.id, result.version)
                    synced++
                }
                is SyncPushResult.Conflict -> {
                    db.paymentDao().markConflict(userId, entity.id)
                    Log.w(TAG, "SyncManager: payment ${entity.id} conflict: ${result.reason}")
                }
                is SyncPushResult.PermanentError -> {
                    db.paymentDao().markError(userId, entity.id)
                    hadPermanentError = true
                    Log.w(TAG, "SyncManager: payment ${entity.id} permanent error: ${result.message}")
                }
                is SyncPushResult.Malformed -> {
                    db.paymentDao().markError(userId, entity.id)
                    hadPermanentError = true
                    Log.w(TAG, "SyncManager: payment ${entity.id} malformed: ${result.detail}")
                }
                is SyncPushResult.NetworkError -> {
                    throw IllegalStateException("unexpected NetworkError from payment pusher")
                }
            }
        }

        Log.d(TAG, "SyncManager: pushed $synced/$total rows (permanent errors: $hadPermanentError)")
        if (hadPermanentError) {
            // Permanent errors are reported but do not fail the whole run - the remaining
            // rows still got pushed. The worker reports SUCCESS so the ERROR rows are not
            // retried forever; they stay ERROR until the user resolves them in 7B.
        }
        return@coroutineScope synced
    }
}