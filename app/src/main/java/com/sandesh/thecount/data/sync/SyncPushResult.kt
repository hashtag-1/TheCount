package com.sandesh.thecount.data.sync

import java.math.BigDecimal

/**
 * Result of ONE push call to the server (thecount_push_stroke / thecount_push_payment).
 * Pure Kotlin: no Android, no network. Network and permanent (Postgres) errors are added in 7A-2c.
 */
sealed interface SyncPushResult {

    /** Server accepted the row and assigned this global version. */
    data class Ok(val version: Long) : SyncPushResult

    /** Server refused to overwrite. [server] holds the server's current values when it sent them. */
    data class Conflict(
        val reason: ConflictReason,
        val version: Long,
        val server: ServerRow?
    ) : SyncPushResult

    /** The response could not be understood. [detail] is a short fixed text, never the raw payload. */
    data class Malformed(val detail: String) : SyncPushResult

    /** A network or transport failure. Retryable. */
    data object NetworkError : SyncPushResult

    /** A Postgres error (bad data, permission denied, RLS). Permanent - do not retry. */
    data class PermanentError(val message: String) : SyncPushResult
}

enum class ConflictReason {
    VERSION_MISMATCH,
    DATE_EXISTS_OTHER_ID,
    INSERT_BLOCKED,

    /** Any reason string this app version does not know. Never crashes. */
    UNKNOWN
}

/** The server's current values for a conflicting row. Every field is nullable on purpose. */
sealed interface ServerRow {

    data class StrokeServerRow(
        val id: String?,
        val strokeCount: Long?,
        val deleted: Boolean?
    ) : ServerRow

    /** [amount] is BigDecimal, scale 2, HALF_UP. Never Double. */
    data class PaymentServerRow(
        val paymentDate: String?,
        val amount: BigDecimal?,
        val note: String?,
        val deleted: Boolean?
    ) : ServerRow
}
