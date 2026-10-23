package com.sandesh.thecount.data.sync

import android.util.Log
import com.sandesh.thecount.data.remote.SupabaseProvider
import com.sandesh.thecount.data.remote.dto.PaymentPushParams
import com.sandesh.thecount.data.remote.dto.StrokePushParams
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pushes one stroke row to the server. Returns a [SyncPushResult] for the caller to act on.
 *
 * The user id is never sent: the server derives it from auth.uid(). Bad data raises a Postgres
 * error, which becomes a [PermanentError]. Network / transport errors are rethrown so the
 * caller (SyncWorker) can classify them.
 */
class StrokePusher {

    suspend fun push(
        id: String,
        workDate: String,
        strokeCount: Long,
        deleted: Boolean,
        baseVersion: Long?
    ): SyncPushResult = withContext(Dispatchers.IO) {
        val params = StrokePushParams(
            p_id = id,
            p_work_date = workDate,
            p_stroke_count = strokeCount,
            p_deleted = deleted,
            p_base_version = baseVersion
        )

        // Encode @Serializable params to JsonObject so it matches the
        // rpc(String, JsonObject, ...) overload exactly.
        val body = Json.encodeToJsonElement(params).jsonObject

        val result = try {
            SupabaseProvider.client.postgrest.rpc("thecount_push_stroke", body) { }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e
        }

        parseStrokePushResponse(result.data)
    }
}

/**
 * Pushes one payment row to the server. Money is sent as a plain text string with 2 decimals
 * (BigDecimal plain form), never as a Double or Float.
 */
class PaymentPusher {

    suspend fun push(
        id: String,
        paymentDate: String,
        amount: BigDecimal,
        note: String?,
        deleted: Boolean,
        baseVersion: Long?
    ): SyncPushResult = withContext(Dispatchers.IO) {
        val params = PaymentPushParams(
            p_id = id,
            p_payment_date = paymentDate,
            p_amount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            p_note = note,
            p_deleted = deleted,
            p_base_version = baseVersion
        )

        val body = Json.encodeToJsonElement(params).jsonObject

        val result = try {
            SupabaseProvider.client.postgrest.rpc("thecount_push_payment", body) { }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e
        }

        parsePaymentPushResponse(result.data)
    }
}
