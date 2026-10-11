package com.sandesh.thecount.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "payment_date") val paymentDate: LocalDate,
    @ColumnInfo(name = "amount") val amount: BigDecimal,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "version") val version: Long = 0L,
    @ColumnInfo(name = "sync_state") val syncState: String = "PENDING_UPSERT",
    @ColumnInfo(name = "server_version") val serverVersion: Long? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null
)
