package com.digitalwellbeing.app

import com.digitalwellbeing.ui.AppUsageBreakdown
import com.digitalwellbeing.ui.HourInsight
import com.digitalwellbeing.ui.MinuteTrendPoint
import com.digitalwellbeing.ui.TrendChart
import com.digitalwellbeing.ui.TrendPoint
import com.digitalwellbeing.ui.TrendSeries
import java.time.LocalDate
import java.time.ZoneId

data class UsageWindow(
    val start: java.time.Instant,
    val end: java.time.Instant,
    val appUsageMs: Map<String, Long>
)

class UsageInsightBuilder(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun buildTrendFromHourlyWindows(
        day: LocalDate,
        hourlyWindows: List<UsageWindow>,
        labelResolver: (String) -> String,
        colorResolver: (String) -> Long
    ): TrendChart {
        val dayWindows = hourlyWindows.filter { it.start.atZone(zoneId).toLocalDate() == day }
        val totalSeries = TrendSeries(
            packageName = null,
            label = "Total",
            colorHex = 0xFFD64C2F,
            points = (0..23).map { localHour ->
                val minutes = dayWindows
                    .filter { it.start.atZone(zoneId).hour == localHour }
                    .sumOf { millisecondsToRoundedMinutes(it.appUsageMs.values.sum()) }
                TrendPoint(localHour, localHour.toString().padStart(2, '0'), minutes, formatMinutes(minutes))
            }
        )

        val appSeries = dayWindows
            .flatMap { it.appUsageMs.entries }
            .groupBy { it.key }
            .map { (packageName, entries) -> packageName to entries.sumOf { millisecondsToRoundedMinutes(it.value) } }
            .sortedByDescending { it.second }
            .take(5)
            .map { (packageName, _) ->
                TrendSeries(
                    packageName = packageName,
                    label = labelResolver(packageName),
                    colorHex = colorResolver(packageName),
                    points = (0..23).map { localHour ->
                        val minutes = dayWindows
                            .filter { it.start.atZone(zoneId).hour == localHour }
                            .sumOf { window -> millisecondsToRoundedMinutes(window.appUsageMs[packageName] ?: 0L) }
                        TrendPoint(localHour, localHour.toString().padStart(2, '0'), minutes, formatMinutes(minutes))
                    }
                )
            }

        return TrendChart(total = totalSeries, apps = appSeries)
    }

    fun buildHourInsight(
        title: String,
        minuteWindows: List<UsageWindow>,
        expectedMinutes: Int,
        labelResolver: (String) -> String,
        colorResolver: (String) -> Long
    ): HourInsight {
        val pieMinutesByPackage = linkedMapOf<String, Int>()
        val minuteTrend = minuteWindows.mapIndexed { index, window ->
            val visibleUsage = window.appUsageMs
                .filterValues { it > 0L }
                .maxByOrNull { it.value }

            window.appUsageMs.forEach { (packageName, millis) ->
                val roundedMinutes = millisecondsToRoundedMinutes(millis)
                if (roundedMinutes > 0) {
                    pieMinutesByPackage[packageName] = (pieMinutesByPackage[packageName] ?: 0) + roundedMinutes
                }
            }

            val totalMillis = window.appUsageMs.values.sum().coerceAtMost(60_000L)
            MinuteTrendPoint(
                minuteIndex = index,
                activeFraction = (totalMillis / 60_000f).coerceIn(0f, 1f),
                colorHex = visibleUsage?.key?.let(colorResolver) ?: 0x33445566,
                label = visibleUsage?.key?.let(labelResolver) ?: "Idle"
            )
        }

        val totalVisibleMinutes = minuteTrend.count { it.activeFraction > 0f }
        val idleMinutes = (expectedMinutes - totalVisibleMinutes).coerceAtLeast(0)
        val pieBreakdown = pieMinutesByPackage
            .entries
            .sortedByDescending { it.value }
            .map { (packageName, minutes) ->
                AppUsageBreakdown(
                    label = labelResolver(packageName),
                    minutes = minutes,
                    colorHex = colorResolver(packageName)
                )
            } + listOf(
            AppUsageBreakdown(
                label = "Idle",
                minutes = idleMinutes,
                colorHex = 0x556B7A8F
            )
        ).filter { it.minutes > 0 }

        return HourInsight(
            title = title,
            subtitle = "${totalVisibleMinutes} visible min · ${idleMinutes} idle min",
            totalVisibleMinutes = totalVisibleMinutes,
            idleMinutes = idleMinutes,
            minuteTrend = minuteTrend,
            pieBreakdown = pieBreakdown
        )
    }

    private fun millisecondsToRoundedMinutes(milliseconds: Long): Int {
        return if (milliseconds <= 0L) 0 else ((milliseconds + 59_999L) / 60_000L).toInt()
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
}
