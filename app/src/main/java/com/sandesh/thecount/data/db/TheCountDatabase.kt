package com.sandesh.thecount.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DailyStrokeEntity::class, PaymentEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class, BigDecimalConverter::class)
abstract class TheCountDatabase : RoomDatabase() {
    abstract fun dailyStrokeDao(): DailyStrokeDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: TheCountDatabase? = null

        fun getInstance(context: Context): TheCountDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TheCountDatabase::class.java,
                    "thecount_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
