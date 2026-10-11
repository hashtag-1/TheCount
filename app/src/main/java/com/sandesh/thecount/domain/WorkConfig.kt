package com.sandesh.thecount.domain

import java.math.BigDecimal
import java.time.LocalDate

object WorkConfig {
    val START_DATE: LocalDate = LocalDate.of(2026, 9, 2)
    val RATE_PER_STROKE: BigDecimal = BigDecimal("0.04")
    val TAX_RATE: BigDecimal = BigDecimal("0.01")
    const val CURRENCY = "रु"
    const val MAX_USERS = 120
    const val ADMIN_CONTACT = ""
}
