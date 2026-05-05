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

    @Query("SELECT * FROM raw_events WHERE timestampEpochMs BETWEEN :startEpochMs AND :endEpochMs ORDER BY timestampEpochMs ASC")
    suspend fun rawEventsBetween(startEpochMs: Long, endEpochMs: Long): List<RawEventEntity>

    @Query("SELECT * FROM app_sessions ORDER BY startEpochMs DESC")
    suspend fun sessions(): List<AppSessionEntity>

    @Query("DELETE FROM raw_events WHERE timestampEpochMs BETWEEN :startEpochMs AND :endEpochMs")
    suspend fun deleteRawEventsBetween(startEpochMs: Long, endEpochMs: Long)

    @Query("DELETE FROM raw_events WHERE timestampEpochMs BETWEEN :startEpochMs AND :endEpochMs AND source = :source")
    suspend fun deleteRawEventsBetweenForSource(startEpochMs: Long, endEpochMs: Long, source: String)

    @Query("DELETE FROM app_sessions WHERE startEpochMs >= :startEpochMs AND endEpochMs <= :endEpochMs")
    suspend fun deleteSessionsBetween(startEpochMs: Long, endEpochMs: Long)

    @Query("DELETE FROM raw_events WHERE timestampEpochMs < :cutoffEpochMs")
    suspend fun purgeRawEventsBefore(cutoffEpochMs: Long)

    @Query("DELETE FROM app_sessions WHERE endEpochMs < :cutoffEpochMs")
    suspend fun purgeSessionsBefore(cutoffEpochMs: Long)
}
