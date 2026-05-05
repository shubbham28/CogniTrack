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
import com.digitalwellbeing.ui.HourInsight
import com.digitalwellbeing.ui.TimelineSlice
import com.digitalwellbeing.ui.TrendChart
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
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
    private val usageInsightBuilder: UsageInsightBuilder,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    private val packageVisibilityCache = mutableMapOf<String, Boolean>()

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
        val weekStart = today.minusDays(6).atStartOfDay(zoneId).toInstant()
        val hourlyUsageWindows = buildUsageWindows(
            start = weekStart,
            end = now,
            step = ChronoUnit.HOURS
        ).map(::filterVisibleUsage)
        val todayHourlyWindows = hourlyUsageWindows.filter { it.start.atZone(zoneId).toLocalDate() == today }
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

        val route = buildRoute(todaySessions).ifEmpty { buildFallbackRoute(todayHourlyWindows) }
        val totalVisibleMinutes = todayHourlyWindows.sumOf { millisecondsToRoundedMinutes(it.appUsageMs.values.sum()) }

        return DashboardState(
            day = today,
            totalMinutes = totalVisibleMinutes.toLong(),
            pickups = summary.totalPickups,
            switches = route.zipWithNext().count { it.first.packageName != it.second.packageName },
            focusScore = summary.focusScore,
            distractionScore = summary.distractionScore,
            cognitiveLoadScore = summary.cognitiveLoadScore,
            timeline = buildTimeline(todayHourlyWindows),
            heatmap = buildHeatmap(hourlyUsageWindows, today),
            trends = buildTrends(todayHourlyWindows, today),
            flow = route
        )
    }

    fun loadCurrentHourInsight(now: Instant = Instant.now()): HourInsight {
        val end = now.truncatedTo(ChronoUnit.MINUTES)
        val start = end.minus(59, ChronoUnit.MINUTES)
        val minuteWindows = buildUsageWindows(start, end.plus(1, ChronoUnit.MINUTES), ChronoUnit.MINUTES)
            .map(::filterVisibleUsage)
        return usageInsightBuilder.buildHourInsight(
            title = "Last 60 min",
            minuteWindows = minuteWindows,
            expectedMinutes = 60,
            labelResolver = ::resolveLabel,
            colorResolver = ::packageColor
        )
    }

    fun loadHourInsight(date: LocalDate, hour: Int): HourInsight {
        val start = date.atTime(hour, 0).atZone(zoneId).toInstant()
        val end = start.plus(1, ChronoUnit.HOURS)
        val minuteWindows = buildUsageWindows(start, end, ChronoUnit.MINUTES)
            .map(::filterVisibleUsage)
        return usageInsightBuilder.buildHourInsight(
            title = "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${formatDate(date)} · ${hour.toString().padStart(2, '0')}:00",
            minuteWindows = minuteWindows,
            expectedMinutes = 60,
            labelResolver = ::resolveLabel,
            colorResolver = ::packageColor
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

    private fun buildTimeline(hourlyWindows: List<UsageWindow>): List<TimelineSlice> {
        return hourlyWindows
            .flatMap { it.appUsageMs.entries }
            .groupBy { it.key }
            .map { (packageName, entries) ->
                TimelineSlice(
                    label = resolveLabel(packageName),
                    minutes = millisecondsToRoundedMinutes(entries.sumOf { it.value }).coerceAtLeast(1),
                    colorHex = packageColor(packageName)
                )
            }
            .sortedByDescending { it.minutes }
            .take(8)
    }

    private fun buildHeatmap(hourlyWindows: List<UsageWindow>, today: LocalDate): List<HeatmapCell> {
        return (0..6).flatMap { dayOffset ->
            val date = today.minusDays((6 - dayOffset).toLong())
            val dayWindows = hourlyWindows.filter { it.start.atZone(zoneId).toLocalDate() == date }
            val maxHourMillis = dayWindows
                .maxOfOrNull { it.appUsageMs.values.sum() }
                ?.coerceAtLeast(1L) ?: 1L

            (0..23).map { hour ->
                val window = dayWindows.firstOrNull { it.start.atZone(zoneId).hour == hour }
                val hourMillis = window?.appUsageMs?.values?.sum() ?: 0L
                HeatmapCell(
                    date = date,
                    dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    dateLabel = formatDate(date),
                    hour = hour,
                    intensity = (hourMillis / maxHourMillis.toFloat()).coerceIn(0f, 1f),
                    minutes = millisecondsToRoundedMinutes(hourMillis),
                    apps = window?.appUsageMs
                        ?.entries
                        ?.map { (packageName, milliseconds) ->
                            AppUsageBreakdown(
                                label = resolveLabel(packageName),
                                minutes = millisecondsToRoundedMinutes(milliseconds),
                                colorHex = packageColor(packageName)
                            )
                        }
                        ?.sortedByDescending { it.minutes }
                        ?.take(4)
                        .orEmpty()
                )
            }
        }
    }

    private fun buildTrends(hourlyWindows: List<UsageWindow>, day: LocalDate): TrendChart {
        return usageInsightBuilder.buildTrendFromHourlyWindows(
            day = day,
            hourlyWindows = hourlyWindows,
            labelResolver = ::resolveLabel,
            colorResolver = ::packageColor
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

    private fun buildFallbackRoute(hourlyWindows: List<UsageWindow>): List<com.digitalwellbeing.capture.AppFlowStep> {
        return hourlyWindows
            .sortedBy { it.start }
            .mapNotNull { window ->
                val topApp = window.appUsageMs.maxByOrNull { it.value } ?: return@mapNotNull null
                com.digitalwellbeing.capture.AppFlowStep(
                    packageName = resolveLabel(topApp.key),
                    sequenceIndex = 0,
                    durationSec = topApp.value / 1000L
                )
            }
            .takeLast(12)
            .mapIndexed { index, step -> step.copy(sequenceIndex = index) }
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
        packageVisibilityCache[packageName]?.let { return it }
        val hiddenPackages = setOf(
            appPackageName,
            "android",
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.as"
        )
        if (packageName in hiddenPackages) {
            packageVisibilityCache[packageName] = false
            return false
        }

        val homePackages = resolveHomePackages()

        if (packageName in homePackages) {
            packageVisibilityCache[packageName] = false
            return false
        }

        val visible = packageManager.getLaunchIntentForPackage(packageName) != null
        packageVisibilityCache[packageName] = visible
        return visible
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

    private fun filterVisibleUsage(window: UsageWindow): UsageWindow {
        return window.copy(
            appUsageMs = window.appUsageMs.filterKeys { shouldShowPackage(it) }
        )
    }

    private fun buildUsageWindows(
        start: Instant,
        end: Instant,
        step: ChronoUnit
    ): List<UsageWindow> {
        val windows = mutableListOf<UsageWindow>()
        var cursor = start
        while (cursor < end) {
            val next = minOf(cursor.plus(1, step), end)
            windows += usageSummaryReader.usageWindow(cursor, next)
            cursor = next
        }
        return windows
    }

    private fun millisecondsToRoundedMinutes(milliseconds: Long): Int {
        return if (milliseconds <= 0L) 0 else ((milliseconds + 59_999L) / 60_000L).toInt()
    }

    private fun formatDate(date: LocalDate): String {
        val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return "$month ${date.dayOfMonth}"
    }
}

object MissingUsageAccessException : IllegalStateException("Usage access is required")
