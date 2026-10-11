package com.sandesh.thecount.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE user_id = :userId AND deleted_at IS NULL ORDER BY payment_date DESC, created_at DESC")
    fun observeAll(userId: String): Flow<List<PaymentEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE user_id = :userId AND deleted_at IS NULL AND payment_date <= :endDate")
    fun sumPaymentsUntil(userId: String, endDate: LocalDate): Flow<BigDecimal>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PaymentEntity)

    @Update
    suspend fun update(entity: PaymentEntity)

    @Query("UPDATE payments SET deleted_at = :deletedAt, sync_state = 'PENDING_DELETE', updated_at = :deletedAt WHERE user_id = :userId AND id = :id AND deleted_at IS NULL")
    suspend fun softDeleteById(userId: String, id: java.util.UUID, deletedAt: Long = System.currentTimeMillis())
}
