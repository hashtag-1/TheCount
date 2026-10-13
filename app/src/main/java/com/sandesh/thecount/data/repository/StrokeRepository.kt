package com.sandesh.thecount.data.repository

import com.sandesh.thecount.data.db.DailyStrokeDao
import com.sandesh.thecount.data.db.DailyStrokeEntity
import com.sandesh.thecount.domain.WorkConfig
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class StrokeRepository(
    private val dao: DailyStrokeDao,
    private val userProvider: CurrentUserProvider
) {
    private val userId: String = userProvider.currentUserId

    fun observeAll(): Flow<List<DailyStrokeEntity>> = dao.observeAll(userId)

    fun sumStrokesUntil(endDate: LocalDate): Flow<Long> =
        dao.sumStrokesUntil(userId, WorkConfig.START_DATE, endDate)

    suspend fun getByDate(date: LocalDate): DailyStrokeEntity? =
        dao.getByDate(userId, date)

    suspend fun saveOrUpdate(workDate: LocalDate, strokeCount: Long): DailyStrokeEntity =
        dao.upsertOrRevive(userId, workDate, strokeCount)

    suspend fun softDelete(workDate: LocalDate) = dao.softDelete(userId, workDate)
}