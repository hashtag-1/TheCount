package com.sandesh.thecount.data.repository

import com.sandesh.thecount.data.db.PaymentDao
import com.sandesh.thecount.data.db.PaymentEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class PaymentRepository(
    private val dao: PaymentDao,
    private val userProvider: CurrentUserProvider
) {
    fun observeAll(): Flow<List<PaymentEntity>> =
        userProvider.currentUserIdFlow.flatMapLatest { userId ->
            dao.observeAll(userId)
        }

    fun sumPaymentsUntil(endDate: LocalDate): Flow<BigDecimal> =
        userProvider.currentUserIdFlow.flatMapLatest { userId ->
            dao.sumPaymentsUntil(userId, endDate)
        }

    suspend fun add(paymentDate: LocalDate, amount: BigDecimal, note: String?): PaymentEntity {
        val userId = userProvider.currentUserId
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

    suspend fun softDelete(id: UUID) {
        val userId = userProvider.currentUserId
        dao.softDeleteById(userId, id)
    }

    // Attach logic (uses "local" explicitly, not current user)
    suspend fun countLocalPayments(): Int = dao.countLocalPayments()

    suspend fun attachLocalPaymentsToUser(targetUserId: String): Int =
        dao.attachLocalPaymentsToUser(targetUserId)
}