package com.sandesh.thecount.data.db

import androidx.room.TypeConverter
import java.math.BigDecimal

object BigDecimalConverter {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.setScale(2, java.math.RoundingMode.HALF_UP)?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it).setScale(2, java.math.RoundingMode.HALF_UP) }
    }
}
