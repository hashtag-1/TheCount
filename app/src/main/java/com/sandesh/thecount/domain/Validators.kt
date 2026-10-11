package com.sandesh.thecount.domain

import com.sandesh.thecount.domain.WorkConfig
import java.time.LocalDate

object Validators {
    fun validateStrokeCount(countStr: String): Result<Long> {
        if (countStr.isBlank()) {
            return Result.failure(IllegalArgumentException("Stroke count is required"))
        }
        val trimmed = countStr.trim()
        // Must be non-negative whole number - no decimals, no non-numeric chars
        // Allow optional leading + (but generally not expected)
        val regex = Regex("^-?\\d+$")
        if (!regex.matches(trimmed)) {
            return Result.failure(IllegalArgumentException("Stroke count must be a whole number"))
        }
        val count = trimmed.toLongOrNull()
            ?: return Result.failure(IllegalArgumentException("Stroke count is too large"))
        if (count < 0) {
            return Result.failure(IllegalArgumentException("Stroke count cannot be negative"))
        }
        return Result.success(count)
    }

    fun validateWorkDate(date: LocalDate): Result<LocalDate> {
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
