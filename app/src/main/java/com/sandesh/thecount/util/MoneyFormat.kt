package com.sandesh.thecount.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

object MoneyFormat {
    fun format(amount: BigDecimal): String {
        val nf = NumberFormat.getNumberInstance(Locale.US)
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
        return "रु " + nf.format(amount.setScale(2, java.math.RoundingMode.HALF_UP))
    }
}
