package com.sandesh.thecount.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStrokeDao {
    @Query("SELECT * FROM daily_strokes WHERE user_id = :userId AND deleted_at IS NULL ORDER BY work_date DESC")
    fun observeAll(userId: String): Flow<List<DailyStrokeEntity>>

    @Query("SELECT * FROM daily_strokes WHERE user_id = :userId AND work_date = :workDate")
    suspend fun getByDate(userId: String, workDate: LocalDate): DailyStrokeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DailyStrokeEntity)

    @Update
    suspend fun update(entity: DailyStrokeEntity)

    @Transaction
    suspend fun upsertOrRevive(userId: String, workDate: LocalDate, strokeCount: Long): DailyStrokeEntity {
        val existing = getByDate(userId, workDate)
        return if (existing == null) {
            val newEntity = DailyStrokeEntity(
                userId = userId,
                workDate = workDate,
                strokeCount = strokeCount,
                syncState = "PENDING_UPSERT"
            )
            insert(newEntity)
            newEntity
        } else {
            val revived = existing.copy(
                strokeCount = strokeCount,
                updatedAt = System.currentTimeMillis(),
                syncState = "PENDING_UPSERT",
                deletedAt = null
            )
            update(revived)
            revived
        }
    }

    @Query("UPDATE daily_strokes SET deleted_at = :deletedAt, sync_state = 'PENDING_DELETE', updated_at = :deletedAt WHERE user_id = :userId AND work_date = :workDate AND deleted_at IS NULL")
    suspend fun softDelete(userId: String, workDate: LocalDate, deletedAt: Long = System.currentTimeMillis())

    @Query("SELECT COALESCE(SUM(stroke_count), 0) FROM daily_strokes WHERE user_id = :userId AND deleted_at IS NULL AND work_date >= :startDate AND work_date <= :endDate")
    fun sumStrokesUntil(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<Long>

    // Attach logic
    @Query("SELECT COUNT(*) FROM daily_strokes WHERE user_id = 'local' AND deleted_at IS NULL")
    suspend fun countLocalStrokes(): Int

    @Query("SELECT work_date FROM daily_strokes WHERE user_id = 'local' AND deleted_at IS NULL")
    suspend fun getLocalStrokeDates(): List<LocalDate>

    @Query("SELECT work_date FROM daily_strokes WHERE user_id = :targetUserId AND work_date IN (:dates) AND deleted_at IS NULL")
    suspend fun getClashingDates(targetUserId: String, dates: List<LocalDate>): List<LocalDate>

    @Transaction
    suspend fun attachLocalToUser(targetUserId: String, dates: List<LocalDate>): Int {
        var updated = 0
        for (date in dates) {
            val entity = getByDate("local", date)
            if (entity != null) {
                val updatedEntity = entity.copy(
                    userId = targetUserId,
                    updatedAt = System.currentTimeMillis(),
                    syncState = "PENDING_UPSERT"
                )
                update(updatedEntity)
                updated++
            }
        }
        return updated
    }
}
