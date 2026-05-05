package com.digitalwellbeing.app

import android.os.Build
import android.content.pm.PackageManager
import com.digitalwellbeing.analytics.DailySummaryCalculator
import com.digitalwellbeing.analytics.DigitalFitnessTranslator
import com.digitalwellbeing.analytics.PickupCounter
import com.digitalwellbeing.analytics.SessionStitcher
import com.digitalwellbeing.capture.AppSession
import com.digitalwellbeing.capture.EventSource
import com.digitalwellbeing.capture.EventType
import com.digitalwellbeing.capture.RawEvent
import com.digitalwellbeing.storage.DigitalWellbeingDao
import com.digitalwellbeing.storage.toDomain
import com.digitalwellbeing.storage.toEntity
import com.digitalwellbeing.ui.DashboardState
import com.digitalwellbeing.ui.HeatmapCell
import com.digitalwellbeing.ui.TimelineSlice
import com.digitalwellbeing.ui.TrendPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

class DashboardRepository(
    private val usageStatsCollector: UsageStatsCollector,
    private val usageSummaryReader: UsageSummaryReader,
    private val permissionGateway: PermissionGateway,
    private val dao: DigitalWellbeingDao,
    private val packageManager: PackageManager,
    private val sessionStitcher: SessionStitcher,
    private val pickupCounter: PickupCounter,
    private val summaryCalculator: DailySummaryCalculator,
    private val translator: DigitalFitnessTranslator,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    suspend fun loadDashboardState(now: Instant = Instant.now()): DashboardState {
        val usageAccess = permissionGateway.hasUsageAccess()
        if (!usageAccess) {
            throw MissingUsageAccessException
        }

        val sevenDaysAgo = now.minus(7, ChronoUnit.DAYS)
        val importedUsageEvents = usageStatsCollector.collect(sevenDaysAgo, now).sortedBy { it.timestamp }
        persistImportWindow(importedUsageEvents, sevenDaysAgo, now)
        val rawEvents = dao.rawEventsBetween(
            sevenDaysAgo.toEpochMilli(),
            now.toEpochMilli()
        ).map { it.toDomain() }
        val sessions = sessionStitcher.stitch(rawEvents)
        persistSessions(sessions, sevenDaysAgo, now)

        val today = now.atZone(zoneId).toLocalDate()
        val todaySessions = sessions.filter { it.startTs.atZone(zoneId).toLocalDate() == today }
        val todayEvents = rawEvents.filter { it.timestamp.atZone(zoneId).toLocalDate() == today }
        val pickups = pickupCounter.count(todayEvents)
        val notifications = todayEvents.count { it.eventType == EventType.NOTIFICATION_POSTED }
        val multitaskingMoments = estimateMultitasking(todaySessions)
        val summary = summaryCalculator.calculate(
            day = today,
            sessions = todaySessions,
            pickups = pickups,
            notifications = notifications,
            multitaskingEvents = multitaskingMoments
        )
        dao.insertDailySummary(summary.toEntity())

        val translated = translator.summarize(
            sessions = todaySessions,
            notificationEvents = notifications,
            multitaskingEvents = multitaskingMoments
        )

        return DashboardState(
            day = today,
            totalMinutes = summary.totalScreenTimeMinutes,
            pickups = summary.totalPickups,
            switches = summary.totalAppSwitches,
            focusScore = summary.focusScore,
            distractionScore = summary.distractionScore,
            cognitiveLoadScore = summary.cognitiveLoadScore,
            timeline = buildTimeline(todaySessions),
            heatmap = buildHeatmap(sessions.filter { it.startTs >= sevenDaysAgo }),
            trends = buildTrends(now),
            flow = translated.route.take(12).map { it.copy(packageName = resolveLabel(it.packageName)) }
        )
    }

    private suspend fun persistImportWindow(
        rawEvents: List<RawEvent>,
        start: Instant,
        end: Instant
    ) {
        dao.deleteRawEventsBetweenForSource(start.toEpochMilli(), end.toEpochMilli(), EventSource.USAGE_STATS.name)
        dao.insertRawEvents(rawEvents.map { it.toEntity() })
    }

    private suspend fun persistSessions(
        sessions: List<AppSession>,
        start: Instant,
        end: Instant
    ) {
        dao.deleteSessionsBetween(start.toEpochMilli(), end.toEpochMilli())
        dao.insertSessions(sessions.map { it.toEntity() })
    }

    private fun buildTimeline(sessions: List<AppSession>): List<TimelineSlice> {
        return sessions
            .groupBy { it.packageName }
            .map { (packageName, appSessions) ->
                TimelineSlice(
                    label = resolveLabel(packageName),
                    minutes = (appSessions.sumOf { it.durationSec } / 60).toInt().coerceAtLeast(1),
                    colorHex = packageColor(packageName)
                )
            }
            .sortedByDescending { it.minutes }
            .take(8)
    }

    private fun buildHeatmap(sessions: List<AppSession>): List<HeatmapCell> {
        val today = LocalDate.now(zoneId)
        return (0..6).flatMap { dayOffset ->
            val date = today.minusDays((6 - dayOffset).toLong())
            val sessionsForDay = sessions.filter { it.startTs.atZone(zoneId).toLocalDate() == date }
            val maxHourMinutes = sessionsForDay
                .groupBy { it.startTs.atZone(zoneId).hour }
                .maxOfOrNull { (_, hourSessions) -> hourSessions.sumOf { it.durationSec } / 60f }
                ?.coerceAtLeast(1f) ?: 1f

            (0..23).map { hour ->
                val hourMinutes = sessionsForDay
                    .filter { it.startTs.atZone(zoneId).hour == hour }
                    .sumOf { it.durationSec } / 60f
                HeatmapCell(
                    dayLabel = date.dayOfWeek.name.take(1),
                    hour = hour,
                    intensity = (hourMinutes / maxHourMinutes).coerceIn(0f, 1f)
                )
            }
        }
    }

    private fun buildTrends(now: Instant): List<TrendPoint> {
        return listOf(7L, 30L, 90L).map { days ->
            val cutoff = now.minus(days, ChronoUnit.DAYS)
            TrendPoint("${days}d", usageSummaryReader.totalMinutesBetween(cutoff, now))
        }
    }

    private fun resolveLabel(packageName: String): String {
        return runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(info).toString()
        }.getOrElse {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    private fun packageColor(packageName: String): Long {
        val palette = listOf(
            0xFFD64C2F,
            0xFF2A3441,
            0xFF9BC53D,
            0xFFDD8C6F,
            0xFF6B7A8F,
            0xFF3D6B80,
            0xFFB56576,
            0xFF588157
        )
        return palette[packageName.hashCode().absoluteValue % palette.size]
    }

    private fun estimateMultitasking(sessions: List<AppSession>): Int {
        return sessions.count { it.durationSec in 1..45 }
    }
}

object MissingUsageAccessException : IllegalStateException("Usage access is required")
