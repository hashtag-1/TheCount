package com.sandesh.thecount.data.repository

import com.sandesh.thecount.data.db.DailyStrokeDao
import com.sandesh.thecount.data.db.PaymentDao
import com.sandesh.thecount.data.db.PaymentEntity
import com.sandesh.thecount.data.db.DailyStrokeEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class AttachManagerTest {

    @Mock
    lateinit var strokeDao: DailyStrokeDao

    @Mock
    lateinit var paymentDao: PaymentDao

    @Mock
    lateinit var strokeRepository: StrokeRepository

    @Mock
    lateinit var paymentRepository: PaymentRepository

    @Mock
    lateinit var userProvider: CurrentUserProvider

    private lateinit var attachManager: AttachManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        attachManager = AttachManager(strokeRepository, paymentRepository)
    }

    @Test
    fun `no local data returns NoLocalData`() = runBlockingTest {
        Mockito.`when`(strokeRepository.countLocalStrokes()).thenReturn(0)
        Mockito.`when`(paymentRepository.countLocalPayments()).thenReturn(0)

        val result = attachManager.tryAttach("user-123")

        Assert.assertTrue(result is AttachResult.NoLocalData)
    }

    @Test
    fun `local data attached successfully returns Attached`() = runBlockingTest {
        Mockito.`when`(strokeRepository.countLocalStrokes()).thenReturn(3)
        Mockito.`when`(paymentRepository.countLocalPayments()).thenReturn(2)
        Mockito.`when`(strokeRepository.getLocalStrokeDates())
            .thenReturn(listOf(LocalDate.parse("2026-09-02"), LocalDate.parse("2026-09-03")))
        Mockito.`when`(strokeRepository.getClashingDates("user-123", any()))
            .thenReturn(emptyList())
        Mockito.`when`(strokeRepository.attachLocalToUser("user-123", any()))
            .thenReturn(3)
        Mockito.`when`(paymentRepository.attachLocalPaymentsToUser("user-123"))
            .thenReturn(2)

        val result = attachManager.tryAttach("user-123")

        Assert.assertTrue(result is AttachResult.Attached)
        val attached = result as AttachResult.Attached
        Assert.assertEquals(3, attached.strokeCount)
        Assert.assertEquals(2, attached.paymentCount)
    }

    @Test
    fun `date clash returns Clash with clashing dates`() = runBlockingTest {
        val clashDate = LocalDate.parse("2026-09-02")
        Mockito.`when`(strokeRepository.countLocalStrokes()).thenReturn(1)
        Mockito.`when`(paymentRepository.countLocalPayments()).thenReturn(0)
        Mockito.`when`(strokeRepository.getLocalStrokeDates())
            .thenReturn(listOf(clashDate))
        Mockito.`when`(strokeRepository.getClashingDates("user-123", listOf(clashDate)))
            .thenReturn(listOf(clashDate))

        val result = attachManager.tryAttach("user-123")

        Assert.assertTrue(result is AttachResult.Clash)
        val clash = result as AttachResult.Clash
        Assert.assertEquals(listOf(clashDate), clash.clashingDates)
    }

    @Test
    fun `local data but target user already has all dates - returns Clash`() = runBlockingTest {
        val existingDate = LocalDate.parse("2026-09-02")
        Mockito.`when`(strokeRepository.countLocalStrokes()).thenReturn(1)
        Mockito.`when`(paymentRepository.countLocalPayments()).thenReturn(0)
        Mockito.`when`(strokeRepository.getLocalStrokeDates())
            .thenReturn(listOf(existingDate))
        Mockito.`when`(strokeRepository.getClashingDates("user-123", listOf(existingDate)))
            .thenReturn(listOf(existingDate))

        val result = attachManager.tryAttach("user-123")

        Assert.assertTrue(result is AttachResult.Clash)
    }
}