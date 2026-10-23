package com.sandesh.thecount.data.sync

/** The four states the SyncChip can show. Lives in data.sync so both SyncRepository and SyncChip can import it. */
sealed interface SyncState {
    data object Synced : SyncState
    data class Pending(val count: Int) : SyncState
    data object Error : SyncState
    data object Offline : SyncState
}
