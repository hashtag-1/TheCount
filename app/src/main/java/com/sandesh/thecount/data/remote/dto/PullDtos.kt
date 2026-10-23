package com.sandesh.thecount.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * A single row returned by SELECT on the daily_strokes table.
 * PostgREST sends only what the authenticated user owns (RLS), so user_id is not needed here.
 * ignoreUnknownKeys is set on the Json instance in Pullers.kt so extra columns are safe.
 *
 * The server has no "deleted" column. A row is deleted when deleted_at is not null.
 */
@Serializable
data class StrokeRowDto(
    val id: String,
    @SerialName("work_date")    val workDate: String,
    @SerialName("stroke_count") val strokeCount: Long,
    @SerialName("deleted_at")   val deletedAt: String? = null,
    val version: Long
)

/**
 * A single row returned by SELECT on the payments table.
 * The amount column is numeric(14,2), so PostgREST sends it as a JSON number (25.5), not a
 * quoted string. JsonPrimitive accepts both forms, and .content gives the exact digits as
 * text, so the value never passes through a Double.
 *
 * The server has no "deleted" column. A row is deleted when deleted_at is not null.
 */
@Serializable
data class PaymentRowDto(
    val id: String,
    @SerialName("payment_date") val paymentDate: String,
    val amount: JsonPrimitive,
    val note: String? = null,
    @SerialName("deleted_at")   val deletedAt: String? = null,
    val version: Long
)