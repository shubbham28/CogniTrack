package com.digitalwellbeing.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DigitalWellbeingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawEvents(events: List<RawEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<AppSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: DailySummaryEntity)

    @Query("SELECT * FROM app_sessions ORDER BY startEpochMs DESC")
    suspend fun sessions(): List<AppSessionEntity>

    @Query("DELETE FROM raw_events WHERE timestampEpochMs < :cutoffEpochMs")
    suspend fun purgeRawEventsBefore(cutoffEpochMs: Long)

    @Query("DELETE FROM app_sessions WHERE endEpochMs < :cutoffEpochMs")
    suspend fun purgeSessionsBefore(cutoffEpochMs: Long)
}
