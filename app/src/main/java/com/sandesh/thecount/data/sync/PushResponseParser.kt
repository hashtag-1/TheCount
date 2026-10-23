package com.sandesh.thecount.data.sync

import com.sandesh.thecount.data.remote.dto.PaymentPushResponseDto
import com.sandesh.thecount.data.remote.dto.StrokePushResponseDto
import com.sandesh.thecount.data.sync.ServerRow.PaymentServerRow
import com.sandesh.thecount.data.sync.ServerRow.StrokeServerRow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure parsing of the two push responses. Never throws for any input and never puts the input
 * text into a result (Malformed.detail is always a short fixed string).
 *
 * Only IllegalArgumentException is caught (SerializationException and NumberFormatException are
 * subclasses of it). CancellationException is NOT a subclass, and nothing here suspends, so it
 * can never be swallowed.
 */

private val pushJson = Json { ignoreUnknownKeys = true }

/** Longest amount text we will even try to convert (guards against absurd exponents). */
private const val MAX_AMOUNT_TEXT_LENGTH = 32

private val PLAIN_DECIMAL = Regex("^-?\\d+(\\.\\d+)?$")

private const val DETAIL_BAD_JSON = "unreadable response"
private const val DETAIL_UNKNOWN_STATUS = "unknown status"
private const val DETAIL_OK_NO_VERSION = "ok without version"
private const val DETAIL_CONFLICT_NO_VERSION = "conflict without version"

fun parseStrokePushResponse(json: String): SyncPushResult {
    val dto = try {
        pushJson.decodeFromString<StrokePushResponseDto>(json)
    } catch (e: IllegalArgumentException) {
        return SyncPushResult.Malformed(DETAIL_BAD_JSON)
    }
    return toResult(
        status = dto.status,
        version = dto.version,
        reason = dto.reason
    ) {
        StrokeServerRow(
            id = dto.serverId,
            strokeCount = dto.serverStrokeCount,
            deleted = dto.serverDeleted
        )
    }
}

fun parsePaymentPushResponse(json: String): SyncPushResult {
    val dto = try {
        pushJson.decodeFromString<PaymentPushResponseDto>(json)
    } catch (e: IllegalArgumentException) {
        return SyncPushResult.Malformed(DETAIL_BAD_JSON)
    }
    return toResult(
        status = dto.status,
        version = dto.version,
        reason = dto.reason
    ) {
        PaymentServerRow(
            paymentDate = dto.serverPaymentDate,
            amount = amountFrom(dto.serverAmount),
            note = dto.serverNote,
            deleted = dto.serverDeleted
        )
    }
}

private fun toResult(
    status: String,
    version: Long?,
    reason: String?,
    serverRow: () -> ServerRow
): SyncPushResult = when (status) {
    "ok" ->
        if (version == null) SyncPushResult.Malformed(DETAIL_OK_NO_VERSION)
        else SyncPushResult.Ok(version)

    "conflict" ->
        if (version == null) SyncPushResult.Malformed(DETAIL_CONFLICT_NO_VERSION)
        else SyncPushResult.Conflict(
            reason = reasonFrom(reason),
            version = version,
            server = serverRow()
        )

    else -> SyncPushResult.Malformed(DETAIL_UNKNOWN_STATUS)
}

private fun reasonFrom(reason: String?): ConflictReason = when (reason) {
    "version_mismatch" -> ConflictReason.VERSION_MISMATCH
    "date_exists_other_id" -> ConflictReason.DATE_EXISTS_OTHER_ID
    "insert_blocked" -> ConflictReason.INSERT_BLOCKED
    else -> ConflictReason.UNKNOWN
}

/** Text -> BigDecimal scale 2 HALF_UP. Returns null for missing, JSON null, or unreadable values. */
private fun amountFrom(primitive: JsonPrimitive?): BigDecimal? {
    if (primitive == null || primitive is JsonNull) return null
    val text = primitive.content.trim()
    // Only plain decimals ("100", "100.5", "-3.25"). Exponent forms like "1E999999999" are
    // rejected BEFORE BigDecimal sees them, because setScale on them would try to build a huge number.
    if (text.length > MAX_AMOUNT_TEXT_LENGTH || !PLAIN_DECIMAL.matches(text)) return null
    return try {
        BigDecimal(text).setScale(2, RoundingMode.HALF_UP)
    } catch (e: NumberFormatException) {
        null
    }
}
