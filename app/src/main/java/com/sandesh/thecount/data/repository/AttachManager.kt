package com.sandesh.thecount.data.repository

import java.time.LocalDate

sealed class AttachResult {
    data class Attached(val strokeCount: Int, val paymentCount: Int) : AttachResult()
    data class Clash(val clashingDates: List<LocalDate>) : AttachResult()
    object NoLocalData : AttachResult()
    object AlreadyAttached : AttachResult()
}

class AttachManager(
    private val strokeRepository: StrokeRepository,
    private val paymentRepository: PaymentRepository
) {

    suspend fun tryAttach(targetUserId: String): AttachResult {
        val localStrokeCount = strokeRepository.countLocalStrokes()
        val localPaymentCount = paymentRepository.countLocalPayments()

        if (localStrokeCount == 0 && localPaymentCount == 0) {
            return AttachResult.NoLocalData
        }

        val localStrokeDates = strokeRepository.getLocalStrokeDates()
        val clashingDates = if (localStrokeDates.isNotEmpty()) {
            strokeRepository.getClashingDates(targetUserId, localStrokeDates)
        } else {
            emptyList()
        }

        if (clashingDates.isNotEmpty()) {
            return AttachResult.Clash(clashingDates)
        }

        val attachedStrokes = strokeRepository.attachLocalToUser(targetUserId, localStrokeDates)
        val attachedPayments = paymentRepository.attachLocalPaymentsToUser(targetUserId)

        return AttachResult.Attached(attachedStrokes, attachedPayments)
    }
}