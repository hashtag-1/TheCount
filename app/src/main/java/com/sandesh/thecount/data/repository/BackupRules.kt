package com.sandesh.thecount.data.repository

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal object BackupRules {

    fun parseDate(value: String): LocalDate =
        LocalDate.parse(value)

    fun validateStrokeCount(count: Long): Long {
        require(count >= 0) { "Stroke count cannot be negative" }
        return count
    }

    fun parsePaymentAmount(value: String): BigDecimal {
        val amount = value.toBigDecimal()
        require(amount >= BigDecimal.ZERO) {
            "Payment amount cannot be negative"
        }
        return amount
    }

    fun addUniqueStrokeDate(
        date: LocalDate,
        dates: MutableSet<LocalDate>
    ) {
        require(dates.add(date)) {
            "Duplicate stroke date found: $date"
        }
    }

    fun shouldApplyImportedRecord(
        mode: BackupMode,
        existingIsDeleted: Boolean
    ): Boolean {
        return mode == BackupMode.REPLACE || !existingIsDeleted
    }
}
