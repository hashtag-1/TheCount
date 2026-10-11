package com.sandesh.thecount.data.repository

import com.sandesh.thecount.data.db.DailyStrokeDao
import com.sandesh.thecount.data.db.DailyStrokeEntity
import com.sandesh.thecount.domain.WorkConfig
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class StrokeRepository(private val dao: DailyStrokeDao) {
    fun observeAll(userId: String): Flow<List<DailyStrokeEntity>> = dao.observeAll(userId)

    fun sumStrokesUntil(userId: String, endDate: LocalDate): Flow<Long> =
        dao.sumStrokesUntil(userId, WorkConfig.START_DATE, endDate)

    suspend fun getByDate(userId: String, date: LocalDate): DailyStrokeEntity? =
        dao.getByDate(userId, date)

    suspend fun saveOrUpdate(userId: String, workDate: LocalDate, strokeCount: Long): DailyStrokeEntity =
        dao.upsertOrRevive(userId, workDate, strokeCount)

    suspend fun softDelete(userId: String, workDate: LocalDate) = dao.softDelete(userId, workDate)
}
