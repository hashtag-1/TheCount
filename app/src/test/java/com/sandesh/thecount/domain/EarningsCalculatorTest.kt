package com.sandesh.thecount.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class EarningsCalculatorTest {
    @Test
    fun zeroStrokes() {
        val result = EarningsCalculator.calculate(0, BigDecimal.ZERO)
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), result.gross)
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), result.tax)
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), result.net)
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), result.outstanding)
    }

    @Test
    fun oneStroke() {
        val result = EarningsCalculator.calculate(1, BigDecimal.ZERO)
        assertEquals(BigDecimal("0.04").setScale(2, RoundingMode.HALF_UP), result.gross)
        assertEquals(BigDecimal("0.00").setScale(2, RoundingMode.HALF_UP), result.tax)
        assertEquals(BigDecimal("0.04").setScale(2, RoundingMode.HALF_UP), result.net)
    }

    @Test
    fun oneHundredThousandStrokes() {
        val result = EarningsCalculator.calculate(100000, BigDecimal.ZERO)
        assertEquals(BigDecimal("4000.00").setScale(2, RoundingMode.HALF_UP), result.gross)
        assertEquals(BigDecimal("40.00").setScale(2, RoundingMode.HALF_UP), result.tax)
        assertEquals(BigDecimal("3960.00").setScale(2, RoundingMode.HALF_UP), result.net)
        assertEquals(BigDecimal("3960.00").setScale(2, RoundingMode.HALF_UP), result.outstanding)
    }

    @Test
    fun oneHundredThousandWithPayments() {
        val result = EarningsCalculator.calculate(100000, BigDecimal("2000.00"))
        assertEquals(BigDecimal("4000.00").setScale(2, RoundingMode.HALF_UP), result.gross)
        assertEquals(BigDecimal("40.00").setScale(2, RoundingMode.HALF_UP), result.tax)
        assertEquals(BigDecimal("3960.00").setScale(2, RoundingMode.HALF_UP), result.net)
        assertEquals(BigDecimal("1960.00").setScale(2, RoundingMode.HALF_UP), result.outstanding)
    }

    @Test
    fun twelveStrokes() {
        val result = EarningsCalculator.calculate(12, BigDecimal.ZERO)
        assertEquals(BigDecimal("0.48").setScale(2, RoundingMode.HALF_UP), result.gross)
        assertEquals(BigDecimal("0.00").setScale(2, RoundingMode.HALF_UP), result.tax)
        assertEquals(BigDecimal("0.48").setScale(2, RoundingMode.HALF_UP), result.net)
    }

    @Test
    fun thirteenStrokes() {
        val result = EarningsCalculator.calculate(13, BigDecimal.ZERO)
        assertEquals(BigDecimal("0.52").setScale(2, RoundingMode.HALF_UP), result.gross)
        assertEquals(BigDecimal("0.01").setScale(2, RoundingMode.HALF_UP), result.tax)
        assertEquals(BigDecimal("0.51").setScale(2, RoundingMode.HALF_UP), result.net)
    }

    @Test
    fun nineBillionStrokes() {
        val result = EarningsCalculator.calculate(9_000_000_000L, BigDecimal.ZERO)
        assertEquals(BigDecimal("360000000.00").setScale(2, RoundingMode.HALF_UP), result.gross)
        assertEquals(BigDecimal("3600000.00").setScale(2, RoundingMode.HALF_UP), result.tax)
        assertEquals(BigDecimal("356400000.00").setScale(2, RoundingMode.HALF_UP), result.net)
    }

    @Test
    fun multiplePayments() {
        val result = EarningsCalculator.calculate(100, BigDecimal("3.00"))
        // 100 strokes = 4.00 gross, tax 0.04, net 3.96, payments 3.00 -> 0.96
        assertEquals(BigDecimal("0.96").setScale(2, RoundingMode.HALF_UP), result.outstanding)
    }

    @Test
    fun paymentsGreaterThanNet() {
        val result = EarningsCalculator.calculate(10, BigDecimal("1.00")) // 0.40 gross, tax 0.00, net 0.40, payments 1.00 -> -0.60
        assertEquals(BigDecimal("-0.60").setScale(2, RoundingMode.HALF_UP), result.outstanding)
    }
}
