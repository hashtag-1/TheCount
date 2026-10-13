package com.sandesh.thecount.data.repository

import com.sandesh.thecount.data.db.DailyStrokeDao
import com.sandesh.thecount.data.db.DailyStrokeEntity
import com.sandesh.thecount.domain.WorkConfig
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class StrokeRepository(
    private val dao: DailyStrokeDao,
    private val userProvider: CurrentUserProvider
) {
    fun observeAll(): Flow<List<DailyStrokeEntity>> =
        userProvider.currentUserIdFlow.flatMapLatest { userId ->
            dao.observeAll(userId)
        }

    fun sumStrokesUntil(endDate: LocalDate): Flow<Long> =
        userProvider.currentUserIdFlow.flatMapLatest { userId ->
            dao.sumStrokesUntil(userId, WorkConfig.START_DATE, endDate)
        }

    suspend fun getByDate(date: LocalDate): DailyStrokeEntity? {
        val userId = userProvider.currentUserId
        return dao.getByDate(userId, date)
    }

    suspend fun saveOrUpdate(workDate: LocalDate, strokeCount: Long): DailyStrokeEntity {
        val userId = userProvider.currentUserId
        return dao.upsertOrRevive(userId, workDate, strokeCount)
    }

    suspend fun softDelete(workDate: LocalDate) {
        val userId = userProvider.currentUserId
        dao.softDelete(userId, workDate)
    }

    // Attach logic (uses "local" explicitly, not current user)
    suspend fun countLocalStrokes(): Int = dao.countLocalStrokes()

    suspend fun getLocalStrokeDates(): List<LocalDate> = dao.getLocalStrokeDates()

    suspend fun getClashingDates(targetUserId: String, dates: List<LocalDate>): List<LocalDate> =
        dao.getClashingDates(targetUserId, dates)

    suspend fun attachLocalToUser(targetUserId: String, dates: List<LocalDate>): Int =
        dao.attachLocalToUser(targetUserId, dates)
}