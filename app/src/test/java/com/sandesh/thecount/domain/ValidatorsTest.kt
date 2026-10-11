package com.sandesh.thecount.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidatorsTest {
    @Test
    fun validateStrokeCount_zero_isValid() {
        val result = Validators.validateStrokeCount("0")
        assertTrue(result.isSuccess)
        assertEquals(0L, result.getOrNull())
    }

    @Test
    fun validateStrokeCount_one_isValid() {
        val result = Validators.validateStrokeCount("1")
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
    }

    @Test
    fun validateStrokeCount_negative_isInvalid() {
        val result = Validators.validateStrokeCount("-1")
        assertTrue(result.isFailure)
    }

    @Test
    fun validateStrokeCount_decimal_isInvalid() {
        val result = Validators.validateStrokeCount("1.5")
        assertTrue(result.isFailure)
    }

    @Test
    fun validateStrokeCount_nonNumeric_isInvalid() {
        val result = Validators.validateStrokeCount("abc")
        assertTrue(result.isFailure)
    }

    @Test
    fun validateStrokeCount_blank_isInvalid() {
        val result = Validators.validateStrokeCount("")
        assertTrue(result.isFailure)
    }

    @Test
    fun validateWorkDate_startDate_isValid() {
        val result = Validators.validateWorkDate(LocalDate.of(2026, 9, 2))
        assertTrue(result.isSuccess)
    }

    @Test
    fun validateWorkDate_dayBeforeStartDate_isInvalid() {
        val result = Validators.validateWorkDate(LocalDate.of(2026, 9, 1))
        assertTrue(result.isFailure)
    }

    @Test
    fun validateWorkDate_tomorrow_isInvalid() {
        val result = Validators.validateWorkDate(LocalDate.now().plusDays(1))
        assertTrue(result.isFailure)
    }
}
