package com.digitalwellbeing.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RawEventEntity::class, AppSessionEntity::class, DailySummaryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DigitalWellbeingDatabase : RoomDatabase() {
    abstract fun wellbeingDao(): DigitalWellbeingDao
}
