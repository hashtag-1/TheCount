package com.sandesh.thecount.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS payments (
                id BLOB NOT NULL PRIMARY KEY,
                user_id TEXT NOT NULL,
                payment_date TEXT NOT NULL,
                amount TEXT NOT NULL,
                note TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                version INTEGER NOT NULL,
                sync_state TEXT NOT NULL,
                server_version INTEGER,
                deleted_at INTEGER
            )
            """.trimIndent()
        )
    }
}
