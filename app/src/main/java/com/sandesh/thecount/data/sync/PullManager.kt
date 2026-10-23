package com.sandesh.thecount.data.sync

import android.util.Log
import com.sandesh.thecount.data.db.DailyStrokeEntity
import com.sandesh.thecount.data.db.PaymentEntity
import com.sandesh.thecount.data.db.TheCountDatabase
import com.sandesh.thecount.data.repository.CurrentUserProvider
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

/**
 * Merges rows fetched from the server into the local Room database.
 *
 * Merge rules (applied per row):
 *  - Row does not exist locally AND server says not deleted  → insert as SYNCED.
 *  - Row does not exist locally AND server says deleted      → nothing (already gone).
 *  - Row exists locally and is PENDING_UPSERT / PENDING_DELETE → skip entirely.
 *    The local version will be pushed on the next push run; it wins.
 *  - Row exists locally and is SYNCED / CONFLICT / ERROR, server version is newer:
 *      · server deleted  → soft-delete the local row, mark SYNCED.
 *      · server alive    → update fields, mark SYNCED, clear deletedAt.
 *  - Row exists locally but server version is NOT newer     → nothing (already up to date).
 *
 * Network errors are rethrown so SyncWorker retries. Decode errors are logged and skipped.
 */
class PullManager(
    private val db: TheCountDatabase,
    private val userProvider: CurrentUserProvider,
    private val strokePuller: StrokePuller = StrokePuller(),
    private val paymentPuller: PaymentPuller = PaymentPuller()
) {

    companion object {
        private const val TAG = "TheCountSync"
        private val PENDING_STATES = setOf("PENDING_UPSERT", "PENDING_DELETE")
    }

    /**
     * Pulls all rows from the server and merges them into Room.
     * Returns the number of rows that were inserted or updated locally.
     * Throws on network error (caller retries).
     */
    suspend fun pullAll(): Int {
        val userId = userProvider.currentUserId
        if (userId == "local" || userId == "anonymous") {
            Log.d(TAG, "PullManager: no signed-in user, skipping pull")
            return 0
        }

        var merged = 0

        // ── Strokes ──────────────────────────────────────────────────────────
        val strokes = try {
            strokePuller.pullAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e     // network failure — worker retries
        }

        Log.d(TAG, "PullManager: fetched ${strokes.size} stroke rows from server")

        for (dto in strokes) {
            try {
                val id = UUID.fromString(dto.id)
                val existing: DailyStrokeEntity? = db.dailyStrokeDao().getById(userId, id)

                if (existing == null) {
                    if (!dto.deleted) {
                        db.dailyStrokeDao().insert(
                            DailyStrokeEntity(
                                id = id,
                                userId = userId,
                                workDate = LocalDate.parse(dto.workDate),
                                strokeCount = dto.strokeCount,
                                syncState = "SYNCED",
                                serverVersion = dto.version
                            )
                        )
                        merged++
                    }
                    // server-deleted + no local row → nothing to do
                } else {
                    if (existing.syncState in PENDING_STATES) continue  // local wins

                    val serverNewer = dto.version > (existing.serverVersion ?: -1L)
                    if (serverNewer) {
                        val now = System.currentTimeMillis()
                        if (dto.deleted) {
                            db.dailyStrokeDao().update(
                                existing.copy(
                                    deletedAt = now,
                                    syncState = "SYNCED",
                                    serverVersion = dto.version,
                                    updatedAt = now
                                )
                            )
                        } else {
                            db.dailyStrokeDao().update(
                                existing.copy(
                                    strokeCount = dto.strokeCount,
                                    deletedAt = null,
                                    syncState = "SYNCED",
                                    serverVersion = dto.version,
                                    updatedAt = now
                                )
                            )
                        }
                        merged++
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "PullManager: skipping stroke ${dto.id}: ${e.message}")
            }
        }

        // ── Payments ─────────────────────────────────────────────────────────
        val payments = try {
            paymentPuller.pullAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e
        }

        Log.d(TAG, "PullManager: fetched ${payments.size} payment rows from server")

        for (dto in payments) {
            try {
                val id = UUID.fromString(dto.id)
                val existing: PaymentEntity? = db.paymentDao().getById(userId, id)
                val serverAmount = BigDecimal(dto.amount).setScale(2, RoundingMode.HALF_UP)

                if (existing == null) {
                    if (!dto.deleted) {
                        db.paymentDao().insert(
                            PaymentEntity(
                                id = id,
                                userId = userId,
                                paymentDate = LocalDate.parse(dto.paymentDate),
                                amount = serverAmount,
                                note = dto.note,
                                syncState = "SYNCED",
                                serverVersion = dto.version
                            )
                        )
                        merged++
                    }
                } else {
                    if (existing.syncState in PENDING_STATES) continue

                    val serverNewer = dto.version > (existing.serverVersion ?: -1L)
                    if (serverNewer) {
                        val now = System.currentTimeMillis()
                        if (dto.deleted) {
                            db.paymentDao().update(
                                existing.copy(
                                    deletedAt = now,
                                    syncState = "SYNCED",
                                    serverVersion = dto.version,
                                    updatedAt = now
                                )
                            )
                        } else {
                            db.paymentDao().update(
                                existing.copy(
                                    amount = serverAmount,
                                    note = dto.note,
                                    paymentDate = LocalDate.parse(dto.paymentDate),
                                    deletedAt = null,
                                    syncState = "SYNCED",
                                    serverVersion = dto.version,
                                    updatedAt = now
                                )
                            )
                        }
                        merged++
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "PullManager: skipping payment ${dto.id}: ${e.message}")
            }
        }

        Log.d(TAG, "PullManager: merged $merged rows into local DB")
        return merged
    }
}
