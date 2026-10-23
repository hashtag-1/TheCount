
package com.sandesh.thecount.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

import java.time.LocalDate

class AttachManagerTest {

    private lateinit var strokeRepository: StrokeRepository
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var attachManager: AttachManager

    @Before
    fun setup() {
        strokeRepository = Mockito.mock(StrokeRepository::class.java)
        paymentRepository = Mockito.mock(PaymentRepository::class.java)
        attachManager = AttachManager(strokeRepository, paymentRepository)
    }

    @Test
    fun `no local data returns NoLocalData`() = runTest {
        Mockito.`when`(strokeRepository.countLocalStrokes()).thenReturn(0)
        Mockito.`when`(paymentRepository.countLocalPayments()).thenReturn(0)

        val result = attachManager.tryAttach("user-123")

        assertTrue(result is AttachResult.NoLocalData)
    }

    @Test
    fun `local data attached successfully returns Attached`() = runTest {
        val dates = listOf(
            LocalDate.parse("2026-09-02"),
            LocalDate.parse("2026-09-03")
        )

        Mockito.`when`(strokeRepository.countLocalStrokes()).thenReturn(3)
        Mockito.`when`(paymentRepository.countLocalPayments()).thenReturn(2)
        Mockito.`when`(strokeRepository.getLocalStrokeDates()).thenReturn(dates)
        Mockito.`when`(
            strokeRepository.getClashingDates("user-123", dates)
        ).thenReturn(emptyList())
        Mockito.`when`(
            strokeRepository.attachLocalToUser("user-123", dates)
        ).thenReturn(3)
        Mockito.`when`(
            paymentRepository.attachLocalPaymentsToUser("user-123")
        ).thenReturn(2)

        val result = attachManager.tryAttach("user-123")

        assertTrue(result is AttachResult.Attached)

        val attached = result as AttachResult.Attached
        assertEquals(3, attached.strokeCount)
        assertEquals(2, attached.paymentCount)
    }

    @Test
    fun `date clash returns Clash with clashing dates`() = runTest {
        val clashDate = LocalDate.parse("2026-09-02")

        Mockito.`when`(strokeRepository.countLocalStrokes()).thenReturn(1)
        Mockito.`when`(paymentRepository.countLocalPayments()).thenReturn(0)
        Mockito.`when`(strokeRepository.getLocalStrokeDates())
            .thenReturn(listOf(clashDate))
        Mockito.`when`(
            strokeRepository.getClashingDates("user-123", listOf(clashDate))
        ).thenReturn(listOf(clashDate))

        val result = attachManager.tryAttach("user-123")

        assertTrue(result is AttachResult.Clash)
        assertEquals(
            listOf(clashDate),
            (result as AttachResult.Clash).clashingDates
        )
    }

    @Test
    fun `local data but target user already has all dates - returns Clash`() = runTest {
        val existingDate = LocalDate.parse("2026-09-02")

        Mockito.`when`(strokeRepository.countLocalStrokes()).thenReturn(1)
        Mockito.`when`(paymentRepository.countLocalPayments()).thenReturn(0)
        Mockito.`when`(strokeRepository.getLocalStrokeDates())
            .thenReturn(listOf(existingDate))
        Mockito.`when`(
            strokeRepository.getClashingDates("user-123", listOf(existingDate))
        ).thenReturn(listOf(existingDate))

        val result = attachManager.tryAttach("user-123")

        assertTrue(result is AttachResult.Clash)
    }
}
