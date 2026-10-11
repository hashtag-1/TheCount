package com.sandesh.thecount.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "daily_strokes",
    indices = [Index(value = ["user_id", "work_date"], unique = true)]
)
data class DailyStrokeEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "work_date") val workDate: LocalDate,
    @ColumnInfo(name = "stroke_count") val strokeCount: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "version") val version: Long = 0L,
    @ColumnInfo(name = "sync_state") val syncState: String = "PENDING_UPSERT",
    @ColumnInfo(name = "server_version") val serverVersion: Long? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null
)
