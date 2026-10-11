package com.sandesh.thecount.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class EarningsResult(
    val strokes: Long,
    val gross: BigDecimal,
    val tax: BigDecimal,
    val net: BigDecimal,
    val outstanding: BigDecimal
)

object EarningsCalculator {
    fun calculate(strokes: Long, payments: BigDecimal): EarningsResult {
        val gross = WorkConfig.RATE_PER_STROKE.multiply(BigDecimal.valueOf(strokes))
        val tax = if (gross.signum() <= 0) {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        } else {
            gross.multiply(WorkConfig.TAX_RATE).setScale(2, RoundingMode.HALF_UP)
        }
        val net = gross.subtract(tax).setScale(2, RoundingMode.HALF_UP)
        val outstanding = net.subtract(payments).setScale(2, RoundingMode.HALF_UP)
        return EarningsResult(
            strokes = strokes,
            gross = gross.setScale(2, RoundingMode.HALF_UP),
            tax = tax,
            net = net,
            outstanding = outstanding
        )
    }
}
