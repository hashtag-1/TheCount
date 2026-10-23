package com.sandesh.thecount.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * JSON returned by thecount_push_stroke.
 * {"status":"ok","version":N} or {"status":"conflict","reason":"...","version":N, server_* ...}
 * Every optional field defaults to null so a missing key never throws.
 */
@Serializable
data class StrokePushResponseDto(
    val status: String,
    val version: Long? = null,
    val reason: String? = null,
    @SerialName("server_id") val serverId: String? = null,
    @SerialName("server_stroke_count") val serverStrokeCount: Long? = null,
    @SerialName("server_deleted") val serverDeleted: Boolean? = null
)

/**
 * JSON returned by thecount_push_payment.
 *
 * server_amount is a JsonPrimitive on purpose: the server sends money as text, and reading
 * .content as a String guarantees the value never passes through Double, quoted or not.
 */
@Serializable
data class PaymentPushResponseDto(
    val status: String,
    val version: Long? = null,
    val reason: String? = null,
    @SerialName("server_payment_date") val serverPaymentDate: String? = null,
    @SerialName("server_amount") val serverAmount: JsonPrimitive? = null,
    @SerialName("server_note") val serverNote: String? = null,
    @SerialName("server_deleted") val serverDeleted: Boolean? = null
)
