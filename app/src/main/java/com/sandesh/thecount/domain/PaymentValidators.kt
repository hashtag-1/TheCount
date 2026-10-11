package com.sandesh.thecount.domain

import java.math.BigDecimal
import java.time.LocalDate

object PaymentValidators {
    fun validateAmount(amountStr: String): Result<BigDecimal> {
        if (amountStr.isBlank()) {
            return Result.failure(IllegalArgumentException("Amount is required"))
        }
        val trimmed = amountStr.trim()
        // Must be valid decimal with at most 2 decimals, > 0
        // Accept formats like 100, 100.0, 100.00 - but reject > 2 decimals
        val regex = Regex("^-?\\d+(\\.\\d{0,2})?$")
        if (!regex.matches(trimmed)) {
            return Result.failure(IllegalArgumentException("Amount must be a valid number with at most 2 decimals"))
        }
        val amount = BigDecimal(trimmed)
        if (amount.scale() > 2) {
            return Result.failure(IllegalArgumentException("Amount can have at most 2 decimals"))
        }
        if (amount <= BigDecimal.ZERO) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        }
        return Result.success(amount.setScale(2, java.math.RoundingMode.HALF_UP))
    }

    fun validateDate(date: LocalDate): Result<LocalDate> {
        if (date.isBefore(WorkConfig.START_DATE)) {
            return Result.failure(IllegalArgumentException("Date cannot be before 2026-09-02"))
        }
        val today = LocalDate.now()
        if (date.isAfter(today)) {
            return Result.failure(IllegalArgumentException("Date cannot be in the future"))
        }
        return Result.success(date)
    }
}
