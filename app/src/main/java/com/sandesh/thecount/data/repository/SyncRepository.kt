package com.sandesh.thecount.data.repository

import com.sandesh.thecount.data.db.TheCountDatabase
import com.sandesh.thecount.data.sync.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Exposes the current sync status for the UI. Pure over Room - no network, no WorkManager.
 * The Home screen shows a SyncChip from this; Settings shows a "Sync now" button that
 * triggers the worker.
 */
class SyncRepository(
    private val db: TheCountDatabase,
    private val userProvider: CurrentUserProvider
) {

    /** Live sync state for the SyncChip. */
    fun syncState(): Flow<SyncState> =
        userProvider.currentUserIdFlow.flatMapLatest { userId ->
            if (userId == "local" || userId == "anonymous") {
                flowOf(SyncState.Synced)
            } else {
                combine(
                    db.dailyStrokeDao().observePendingCount(userId),
                    db.paymentDao().observePendingCount(userId)
                ) { strokes, payments -> strokes + payments }
                    .map { count ->
                        when {
                            count == 0 -> SyncState.Synced
                            else -> SyncState.Pending(count)
                        }
                    }
            }
        }
}