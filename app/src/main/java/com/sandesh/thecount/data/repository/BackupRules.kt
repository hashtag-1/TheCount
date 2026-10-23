package com.sandesh.thecount.data.repository

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal object BackupRules {

    val START_DATE: LocalDate = LocalDate.of(2026, 9, 2)

    fun parseDate(value: String): LocalDate = LocalDate.parse(value)

    fun validateDate(date: LocalDate, today: LocalDate) {
        require(!date.isBefore(START_DATE)) { "Date before start date" }
        require(!date.isAfter(today)) { "Date in the future" }
    }

    fun validateStrokeCount(count: Long) {
        require(count >= 0) { "Stroke count cannot be negative" }
    }

    fun parsePaymentAmount(value: String): BigDecimal {
        val amount = try { value.toBigDecimal() } catch (e: Exception) { throw IllegalArgumentException("Invalid amount", e) }
        require(amount.compareTo(BigDecimal.ZERO) > 0) { "Payment amount must be greater than 0" }
        require(amount.scale() <= 2) { "Payment amount has too many decimals" }
        return amount
    }

    fun addUniqueStrokeDate(date: LocalDate, dates: MutableSet<LocalDate>) {
        require(dates.add(date)) { "Duplicate stroke date found: $date" }
    }

    fun addUniqueId(id: UUID, ids: MutableSet<UUID>) {
        require(ids.add(id)) { "Duplicate ID found: $id" }
    }

    fun shouldApplyImportedRecord(mode: BackupMode, existingIsDeleted: Boolean): Boolean {
        return mode == BackupMode.REPLACE || existingIsDeleted
    }
}
