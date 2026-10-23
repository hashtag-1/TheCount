package com.sandesh.thecount.data.remote.dto

import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Push request payloads for the two server functions. Field names match the server
 * parameters EXACTLY (thecount_push_stroke / thecount_push_payment). The user id is
 * never sent: the server uses auth.uid() on every call.
 */
@Serializable
data class StrokePushParams(
    val p_id: String,
    val p_work_date: String,
    val p_stroke_count: Long,
    val p_deleted: Boolean,
    val p_base_version: Long?
)

@Serializable
data class PaymentPushParams(
    val p_id: String,
    val p_payment_date: String,
    val p_amount: String,
    val p_note: String?,
    val p_deleted: Boolean,
    val p_base_version: Long?
)