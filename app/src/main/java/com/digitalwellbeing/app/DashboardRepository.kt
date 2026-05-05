package com.digitalwellbeing.app

import android.os.Build
import android.content.Intent
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
import com.digitalwellbeing.ui.AppUsageBreakdown
import com.digitalwellbeing.ui.DashboardState
import com.digitalwellbeing.ui.HeatmapCell
import com.digitalwellbeing.ui.TimelineSlice
import com.digitalwellbeing.ui.TrendChart
import com.digitalwellbeing.ui.TrendPoint
import com.digitalwellbeing.ui.TrendSeries
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.absoluteValue

class DashboardRepository(
    private val usageStatsCollector: UsageStatsCollector,
    private val usageSummaryReader: UsageSummaryReader,
    private val permissionGateway: PermissionGateway,
    private val dao: DigitalWellbeingDao,
    private val packageManager: PackageManager,
    private val appPackageName: String,
    private val sessionStitcher: SessionStitcher,
    private val pickupCounter: PickupCounter,
    private val summaryCalculator: DailySummaryCalculator,
    private val translator: DigitalFitnessTranslator,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    private data class HourSlice(
        val date: LocalDate,
        val hour: Int,
        val packageName: String,
        val seconds: Long
    )

    fun statusLine(): String {
        return if (permissionGateway.hasNotificationAccess()) {
            "Imported live UsageStats, screen events, and notification load"
        } else {
            "Imported live UsageStats and screen events. Enable notification access for real cognitive load."
        }
    }

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
        val visibleSessions = sessions.filter { shouldShowPackage(it.packageName) }
        val todaySessions = visibleSessions.filter { it.startTs.atZone(zoneId).toLocalDate() == today }
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

        val route = buildRoute(todaySessions)

        return DashboardState(
            day = today,
            totalMinutes = summary.totalScreenTimeMinutes,
            pickups = summary.totalPickups,
            switches = route.zipWithNext().count { it.first.packageName != it.second.packageName },
            focusScore = summary.focusScore,
            distractionScore = summary.distractionScore,
            cognitiveLoadScore = summary.cognitiveLoadScore,
            timeline = buildTimeline(todaySessions),
            heatmap = buildHeatmap(visibleSessions, today),
            trends = buildTrends(todaySessions, today),
            flow = route
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

    private fun buildHeatmap(sessions: List<AppSession>, today: LocalDate): List<HeatmapCell> {
        val hourSlices = splitIntoHourSlices(sessions)
        return (0..6).flatMap { dayOffset ->
            val date = today.minusDays((6 - dayOffset).toLong())
            val daySlices = hourSlices.filter { it.date == date }
            val maxHourSeconds = daySlices
                .groupBy { it.hour }
                .maxOfOrNull { (_, slices) -> slices.sumOf { it.seconds } }
                ?.coerceAtLeast(1L) ?: 1L

            (0..23).map { hour ->
                val slicesForHour = daySlices.filter { it.hour == hour }
                val hourSeconds = slicesForHour.sumOf { it.seconds }
                HeatmapCell(
                    dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    dateLabel = formatDate(date),
                    hour = hour,
                    intensity = (hourSeconds / maxHourSeconds.toFloat()).coerceIn(0f, 1f),
                    minutes = secondsToMinutes(hourSeconds),
                    apps = slicesForHour
                        .groupBy { it.packageName }
                        .map { (packageName, appSlices) ->
                            AppUsageBreakdown(
                                label = resolveLabel(packageName),
                                minutes = secondsToMinutes(appSlices.sumOf { it.seconds }),
                                colorHex = packageColor(packageName)
                            )
                        }
                        .sortedByDescending { it.minutes }
                        .take(4)
                )
            }
        }
    }

    private fun buildTrends(sessions: List<AppSession>, day: LocalDate): TrendChart {
        val hourSlices = splitIntoHourSlices(sessions).filter { it.date == day }
        val totalSeries = TrendSeries(
            packageName = null,
            label = "Total",
            colorHex = 0xFFD64C2F,
            points = buildHourlyPoints(hourSlices)
        )
        val appSeries = sessions
            .groupBy { it.packageName }
            .map { (packageName, appSessions) -> packageName to appSessions.sumOf { it.durationSec } }
            .sortedByDescending { it.second }
            .take(5)
            .map { (packageName, _) ->
                TrendSeries(
                    packageName = packageName,
                    label = resolveLabel(packageName),
                    colorHex = packageColor(packageName),
                    points = buildHourlyPoints(hourSlices.filter { it.packageName == packageName })
                )
            }
        return TrendChart(
            total = totalSeries,
            apps = appSeries
        )
    }

    private fun buildRoute(sessions: List<AppSession>): List<com.digitalwellbeing.capture.AppFlowStep> {
        val mergedSessions = mutableListOf<AppSession>()
        sessions
            .sortedBy { it.startTs }
            .filter { it.durationSec >= 20 }
            .forEach { session ->
                val last = mergedSessions.lastOrNull()
                if (
                    last != null &&
                    last.packageName == session.packageName &&
                    Duration.between(last.endTs, session.startTs).abs() <= Duration.ofMinutes(2)
                ) {
                    mergedSessions[mergedSessions.lastIndex] = last.copy(
                        endTs = session.endTs,
                        durationSec = last.durationSec + session.durationSec
                    )
                } else {
                    mergedSessions += session
                }
            }

        return mergedSessions
            .takeLast(12)
            .mapIndexed { index, session ->
                com.digitalwellbeing.capture.AppFlowStep(
                    packageName = resolveLabel(session.packageName),
                    sequenceIndex = index,
                    durationSec = session.durationSec
                )
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

    private fun shouldShowPackage(packageName: String): Boolean {
        val hiddenPackages = setOf(
            appPackageName,
            "android",
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.as"
        )
        if (packageName in hiddenPackages) return false

        val homePackages = resolveHomePackages()

        if (packageName in homePackages) return false

        return packageManager.getLaunchIntentForPackage(packageName) != null
    }

    private fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val remainder = minutes % 60
        return when {
            hours > 0 && remainder > 0 -> "${hours}h ${remainder}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    private fun resolveHomePackages(): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(homeIntent, 0)
        }
        return activities.map { it.activityInfo.packageName }.toSet()
    }

    private fun splitIntoHourSlices(sessions: List<AppSession>): List<HourSlice> {
        return sessions.flatMap { session ->
            val sessionStart = session.startTs.atZone(zoneId)
            val sessionEnd = session.endTs.atZone(zoneId)
            if (!sessionEnd.isAfter(sessionStart)) return@flatMap emptyList()

            buildList {
                var cursor = sessionStart
                while (cursor.isBefore(sessionEnd)) {
                    val nextHour = cursor.truncatedTo(ChronoUnit.HOURS).plusHours(1)
                    val sliceEnd = minOf(nextHour, sessionEnd)
                    val seconds = Duration.between(cursor, sliceEnd).seconds.coerceAtLeast(0)
                    if (seconds > 0) {
                        add(
                            HourSlice(
                                date = cursor.toLocalDate(),
                                hour = cursor.hour,
                                packageName = session.packageName,
                                seconds = seconds
                            )
                        )
                    }
                    cursor = sliceEnd
                }
            }
        }
    }

    private fun buildHourlyPoints(hourSlices: List<HourSlice>): List<TrendPoint> {
        return (0..23).map { hour ->
            val minutes = secondsToMinutes(hourSlices.filter { it.hour == hour }.sumOf { it.seconds })
            TrendPoint(
                hour = hour,
                label = hour.toString().padStart(2, '0'),
                value = minutes,
                formattedValue = formatMinutes(minutes)
            )
        }
    }

    private fun secondsToMinutes(seconds: Long): Int {
        return if (seconds <= 0) 0 else ((seconds + 59) / 60).toInt()
    }

    private fun formatDate(date: LocalDate): String {
        val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return "$month ${date.dayOfMonth}"
    }
}

object MissingUsageAccessException : IllegalStateException("Usage access is required")
