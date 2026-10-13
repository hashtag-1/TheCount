package com.sandesh.thecount.data.repository

import com.sandesh.thecount.data.db.PaymentDao
import com.sandesh.thecount.data.db.PaymentEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class PaymentRepository(
    private val dao: PaymentDao,
    private val userProvider: CurrentUserProvider
) {
    private val userId: String = userProvider.currentUserId

    fun observeAll(): Flow<List<PaymentEntity>> = dao.observeAll(userId)

    fun sumPaymentsUntil(endDate: LocalDate): Flow<BigDecimal> =
        dao.sumPaymentsUntil(userId, endDate)

    suspend fun add(paymentDate: LocalDate, amount: BigDecimal, note: String?): PaymentEntity {
        val entity = PaymentEntity(
            userId = userId,
            paymentDate = paymentDate,
            amount = amount,
            note = note
        )
        dao.insert(entity)
        return entity
    }

    suspend fun update(entity: PaymentEntity) {
        dao.update(entity.copy(updatedAt = System.currentTimeMillis(), syncState = "PENDING_UPSERT"))
    }

    suspend fun softDelete(id: UUID) = dao.softDeleteById(userId, id)
}