package com.sandesh.thecount.data.sync

import com.sandesh.thecount.data.sync.ServerRow.PaymentServerRow
import com.sandesh.thecount.data.sync.ServerRow.StrokeServerRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PushResponseParserTest {

    // ---------- helpers ----------

    private fun strokeConflict(json: String): SyncPushResult.Conflict {
        val r = parseStrokePushResponse(json)
        assertTrue("expected Conflict but was $r", r is SyncPushResult.Conflict)
        return r as SyncPushResult.Conflict
    }

    private fun paymentConflict(json: String): SyncPushResult.Conflict {
        val r = parsePaymentPushResponse(json)
        assertTrue("expected Conflict but was $r", r is SyncPushResult.Conflict)
        return r as SyncPushResult.Conflict
    }

    private fun paymentRow(json: String): PaymentServerRow =
        paymentConflict(json).server as PaymentServerRow

    private fun paymentConflictJson(amountPart: String): String =
        """{"status":"conflict","reason":"version_mismatch","version":7,$amountPart}"""

    // ---------- stroke ----------

    @Test
    fun stroke_ok_returnsOkWithVersion() {
        val r = parseStrokePushResponse("""{"status":"ok","version":42}""")
        assertEquals(SyncPushResult.Ok(42L), r)
    }

    @Test
    fun stroke_ok_largeVersion_keepsExactLong() {
        val r = parseStrokePushResponse("""{"status":"ok","version":9007199254740993}""")
        assertEquals(SyncPushResult.Ok(9007199254740993L), r)
    }

    @Test
    fun stroke_ok_withoutVersion_isMalformed() {
        val r = parseStrokePushResponse("""{"status":"ok"}""")
        assertTrue(r is SyncPushResult.Malformed)
    }

    @Test
    fun stroke_conflict_versionMismatch_carriesServerValues() {
        val c = strokeConflict(
            """{"status":"conflict","reason":"version_mismatch","version":9,
                "server_id":"11111111-1111-1111-1111-111111111111",
                "server_stroke_count":1500,"server_deleted":false}"""
        )
        assertEquals(ConflictReason.VERSION_MISMATCH, c.reason)
        assertEquals(9L, c.version)
        assertEquals(
            StrokeServerRow("11111111-1111-1111-1111-111111111111", 1500L, false),
            c.server
        )
    }

    @Test
    fun stroke_conflict_dateExistsOtherId_carriesOtherRowId() {
        val c = strokeConflict(
            """{"status":"conflict","reason":"date_exists_other_id","version":3,
                "server_id":"22222222-2222-2222-2222-222222222222",
                "server_stroke_count":10,"server_deleted":true}"""
        )
        assertEquals(ConflictReason.DATE_EXISTS_OTHER_ID, c.reason)
        val row = c.server as StrokeServerRow
        assertEquals("22222222-2222-2222-2222-222222222222", row.id)
        assertEquals(true, row.deleted)
    }

    @Test
    fun stroke_conflict_insertBlocked_parses() {
        val c = strokeConflict(
            """{"status":"conflict","reason":"insert_blocked","version":5}"""
        )
        assertEquals(ConflictReason.INSERT_BLOCKED, c.reason)
        assertEquals(5L, c.version)
    }

    @Test
    fun stroke_conflict_unknownReason_mapsToUnknown() {
        val c = strokeConflict(
            """{"status":"conflict","reason":"something_new","version":5}"""
        )
        assertEquals(ConflictReason.UNKNOWN, c.reason)
    }

    @Test
    fun stroke_conflict_missingReason_mapsToUnknown() {
        val c = strokeConflict("""{"status":"conflict","version":5}""")
        assertEquals(ConflictReason.UNKNOWN, c.reason)
    }

    @Test
    fun stroke_conflict_withoutVersion_isMalformed() {
        val r = parseStrokePushResponse(
            """{"status":"conflict","reason":"version_mismatch"}"""
        )
        assertTrue(r is SyncPushResult.Malformed)
    }

    @Test
    fun stroke_unknownExtraFields_areIgnored() {
        val r = parseStrokePushResponse(
            """{"status":"ok","version":1,"future_field":{"a":[1,2,3]},"x":"y"}"""
        )
        assertEquals(SyncPushResult.Ok(1L), r)
    }

    @Test
    fun stroke_conflict_missingOptionalServerFields_giveNulls() {
        val c = strokeConflict(
            """{"status":"conflict","reason":"version_mismatch","version":2}"""
        )
        assertEquals(StrokeServerRow(null, null, null), c.server)
    }

    // ---------- payment ----------

    @Test
    fun payment_ok_returnsOk() {
        val r = parsePaymentPushResponse("""{"status":"ok","version":100}""")
        assertEquals(SyncPushResult.Ok(100L), r)
    }

    @Test
    fun payment_conflict_versionMismatch_parsesAllServerFields() {
        val row = paymentRow(
            """{"status":"conflict","reason":"version_mismatch","version":8,
                "server_payment_date":"2026-09-10","server_amount":"1200.00",
                "server_note":"cash","server_deleted":false}"""
        )
        assertEquals("2026-09-10", row.paymentDate)
        assertEquals(BigDecimal("1200.00"), row.amount)
        assertEquals("cash", row.note)
        assertEquals(false, row.deleted)
    }

    @Test
    fun payment_amount_shortText_getsScale2() {
        val row = paymentRow(paymentConflictJson(""""server_amount":"100.5""""))
        assertEquals(BigDecimal("100.50"), row.amount)
        assertEquals(2, row.amount!!.scale())
    }

    @Test
    fun payment_amount_roundsHalfUp() {
        val row = paymentRow(paymentConflictJson(""""server_amount":"100.555""""))
        assertEquals(BigDecimal("100.56"), row.amount)
    }

    @Test
    fun payment_amount_unquotedNumber_isExactNotDouble() {
        val row = paymentRow(paymentConflictJson(""""server_amount":250.5"""))
        assertEquals(BigDecimal("250.50"), row.amount)
        assertEquals("250.50", row.amount!!.toPlainString())
    }

    @Test
    fun payment_amount_unquotedLongDecimal_notDistortedByDouble() {
        // 0.1 + 0.2 style values must survive exactly; as a Double this would not.
        val row = paymentRow(paymentConflictJson(""""server_amount":12345678901234567.89"""))
        assertEquals(BigDecimal("12345678901234567.89"), row.amount)
    }

    @Test
    fun payment_amount_invalidText_givesNullAmountNoThrow() {
        val row = paymentRow(paymentConflictJson(""""server_amount":"abc""""))
        assertNull(row.amount)
    }

    @Test
    fun payment_amount_emptyText_givesNullAmount() {
        val row = paymentRow(paymentConflictJson(""""server_amount":"""""))
        assertNull(row.amount)
    }

    @Test
    fun payment_amount_absurdExponent_givesNullAmountNoThrow() {
        val row = paymentRow(paymentConflictJson(""""server_amount":"1E999999999""""))
        assertNull(row.amount)
    }

    @Test
    fun payment_amount_arrayInsteadOfPrimitive_isMalformed() {
        val r = parsePaymentPushResponse(
            """{"status":"conflict","version":1,"server_amount":[1,2]}"""
        )
        assertTrue(r is SyncPushResult.Malformed)
    }

    @Test
    fun payment_amount_negativePlainDecimal_keptAsIs() {
        val row = paymentRow(paymentConflictJson(""""server_amount":"-3.255""""))
        assertEquals(BigDecimal("-3.26"), row.amount)
    }

    @Test
    fun payment_amount_jsonNull_givesNullAmount() {
        val row = paymentRow(paymentConflictJson(""""server_amount":null"""))
        assertNull(row.amount)
    }

    @Test
    fun payment_amount_missing_givesNullAmount() {
        val row = paymentRow(paymentConflictJson(""""server_note":"x""""))
        assertNull(row.amount)
    }

    @Test
    fun payment_note_null_and_nonNull_bothParse() {
        val withNull = paymentRow(paymentConflictJson(""""server_note":null"""))
        assertNull(withNull.note)
        val withText = paymentRow(paymentConflictJson(""""server_note":"rent""""))
        assertEquals("rent", withText.note)
    }

    @Test
    fun payment_deleted_trueAndFalseParse() {
        val t = paymentRow(paymentConflictJson(""""server_deleted":true"""))
        assertEquals(true, t.deleted)
        val f = paymentRow(paymentConflictJson(""""server_deleted":false"""))
        assertEquals(false, f.deleted)
    }

    @Test
    fun payment_conflict_insertBlocked_parses() {
        val c = paymentConflict(
            """{"status":"conflict","reason":"insert_blocked","version":4}"""
        )
        assertEquals(ConflictReason.INSERT_BLOCKED, c.reason)
    }

    @Test
    fun payment_conflict_withoutVersion_isMalformed() {
        val r = parsePaymentPushResponse("""{"status":"conflict","reason":"version_mismatch"}""")
        assertTrue(r is SyncPushResult.Malformed)
    }

    // ---------- both ----------

    private val badInputs = listOf(
        "",
        "not json",
        "{}",
        "[]",
        "null",
        """{"status":"weird"}""",
        """{"status":123}""",
        """{"status":"ok","version":"abc"}"""
    )

    @Test
    fun bothParsers_badInput_returnMalformedAndNeverThrow() {
        for (input in badInputs) {
            val s = parseStrokePushResponse(input)
            val p = parsePaymentPushResponse(input)
            assertTrue("stroke input <$input> gave $s", s is SyncPushResult.Malformed)
            assertTrue("payment input <$input> gave $p", p is SyncPushResult.Malformed)
        }
    }

    @Test
    fun bothParsers_malformedDetail_neverContainsInputText() {
        val inputs = listOf(
            "not json SECRET123",
            """{"status":"SECRET123"}""",
            """{"status":"ok","version":"SECRET123"}"""
        )
        for (input in inputs) {
            val s = parseStrokePushResponse(input) as SyncPushResult.Malformed
            val p = parsePaymentPushResponse(input) as SyncPushResult.Malformed
            assertFalse(s.detail.contains("SECRET123"))
            assertFalse(p.detail.contains("SECRET123"))
        }
    }
}
