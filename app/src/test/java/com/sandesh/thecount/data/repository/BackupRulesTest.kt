package com.sandesh.thecount.data.repository

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BackupRulesTest {
    @Test
    fun dateBoundary() {
        BackupRules.validateDate(LocalDate.of(2026,9,2), LocalDate.of(2026,9,2))
        try { BackupRules.validateDate(LocalDate.of(2026,9,1), LocalDate.of(2026,9,2)); fail() } catch (e: IllegalArgumentException) {}
    }

    @Test
    fun futureDate() {
        try { BackupRules.validateDate(LocalDate.of(2026,9,3), LocalDate.of(2026,9,2)); fail() } catch (e: IllegalArgumentException) {}
    }

    @Test
    fun strokeCount() {
        BackupRules.validateStrokeCount(0)
        try { BackupRules.validateStrokeCount(-1); fail() } catch (e: IllegalArgumentException) {}
    }

    @Test
    fun paymentAmount() {
        assertEquals(BigDecimal("0.01"), BackupRules.parsePaymentAmount("0.01"))
        try { BackupRules.parsePaymentAmount("0"); fail() } catch (e: IllegalArgumentException) {}
        try { BackupRules.parsePaymentAmount("0.00"); fail() } catch (e: IllegalArgumentException) {}
        try { BackupRules.parsePaymentAmount("-1"); fail() } catch (e: IllegalArgumentException) {}
        try { BackupRules.parsePaymentAmount("1.234"); fail() } catch (e: IllegalArgumentException) {}
        try { BackupRules.parsePaymentAmount("abc"); fail() } catch (e: IllegalArgumentException) {}
    }

    @Test
    fun duplicateDate() {
        val set = mutableSetOf<LocalDate>()
        BackupRules.addUniqueStrokeDate(LocalDate.of(2026,9,2), set)
        try { BackupRules.addUniqueStrokeDate(LocalDate.of(2026,9,2), set); fail() } catch (e: IllegalArgumentException) {}
    }

    @Test
    fun mergeDecision() {
        assertFalse(BackupRules.shouldApplyImportedRecord(BackupMode.MERGE, false))
        assertTrue(BackupRules.shouldApplyImportedRecord(BackupMode.MERGE, true))
        assertTrue(BackupRules.shouldApplyImportedRecord(BackupMode.REPLACE, false))
    }
}
