package com.digitalwellbeing.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.digitalwellbeing.capture.AppSession
import com.digitalwellbeing.capture.DailySummary
import com.digitalwellbeing.capture.EventSource
import com.digitalwellbeing.capture.EventType
import com.digitalwellbeing.capture.RawEvent
import com.digitalwellbeing.capture.SessionConfidence
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "raw_events")
data class RawEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMs: Long,
    val eventType: String,
    val packageName: String?,
    val source: String
)

@Entity(tableName = "app_sessions")
data class AppSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationSec: Long,
    val screenWindowId: Long,
    val confidence: String
)

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey val day: String,
    val totalScreenTimeMinutes: Long,
    val totalPickups: Int,
    val totalAppSwitches: Int,
    val focusScore: Int,
    val distractionScore: Int,
    val cognitiveLoadScore: Int
)

fun RawEventEntity.toDomain(): RawEvent = RawEvent(
    timestamp = Instant.ofEpochMilli(timestampEpochMs),
    eventType = EventType.valueOf(eventType),
    packageName = packageName,
    source = EventSource.valueOf(source)
)

fun RawEvent.toEntity(): RawEventEntity = RawEventEntity(
    timestampEpochMs = timestamp.toEpochMilli(),
    eventType = eventType.name,
    packageName = packageName,
    source = source.name
)

fun AppSessionEntity.toDomain(): AppSession = AppSession(
    packageName = packageName,
    startTs = Instant.ofEpochMilli(startEpochMs),
    endTs = Instant.ofEpochMilli(endEpochMs),
    durationSec = durationSec,
    screenWindowId = screenWindowId,
    confidence = SessionConfidence.valueOf(confidence)
)

fun AppSession.toEntity(): AppSessionEntity = AppSessionEntity(
    packageName = packageName,
    startEpochMs = startTs.toEpochMilli(),
    endEpochMs = endTs.toEpochMilli(),
    durationSec = durationSec,
    screenWindowId = screenWindowId,
    confidence = confidence.name
)

fun DailySummaryEntity.toDomain(): DailySummary = DailySummary(
    day = LocalDate.parse(day),
    totalScreenTimeMinutes = totalScreenTimeMinutes,
    totalPickups = totalPickups,
    totalAppSwitches = totalAppSwitches,
    focusScore = focusScore,
    distractionScore = distractionScore,
    cognitiveLoadScore = cognitiveLoadScore
)

fun DailySummary.toEntity(): DailySummaryEntity = DailySummaryEntity(
    day = day.toString(),
    totalScreenTimeMinutes = totalScreenTimeMinutes,
    totalPickups = totalPickups,
    totalAppSwitches = totalAppSwitches,
    focusScore = focusScore,
    distractionScore = distractionScore,
    cognitiveLoadScore = cognitiveLoadScore
)
