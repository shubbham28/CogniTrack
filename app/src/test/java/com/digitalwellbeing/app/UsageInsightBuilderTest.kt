package com.digitalwellbeing.app

import com.digitalwellbeing.ui.HourInsight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class UsageInsightBuilderTest {
    private val zoneId = ZoneId.of("Europe/Dublin")
    private val builder = UsageInsightBuilder(zoneId)

    @Test
    fun `buildHourInsight calculates app breakdown and idle minutes from minute windows`() {
        val start = Instant.parse("2026-05-05T14:00:00Z")
        val minuteWindows = listOf(
            usageWindow(start, "chat" to 60_000L),
            usageWindow(start.plusSeconds(60), "maps" to 60_000L),
            usageWindow(start.plusSeconds(120), "chat" to 30_000L)
        )

        val insight = builder.buildHourInsight(
            title = "Mon May 5 · 15:00",
            minuteWindows = minuteWindows,
            expectedMinutes = 4,
            labelResolver = { it.replaceFirstChar(Char::uppercase) },
            colorResolver = { if (it == "chat") 0xFF0000 else 0x00FF00 }
        )

        assertEquals(3, insight.totalVisibleMinutes)
        assertEquals(1, insight.idleMinutes)
        assertEquals(listOf("Chat", "Maps", "Idle"), insight.pieBreakdown.map { it.label })
        assertEquals(listOf(2, 1, 1), insight.pieBreakdown.map { it.minutes })
    }

    @Test
    fun `buildTrendFromHourlyWindows uses aggregated foreground minutes per app and hour`() {
        val day = LocalDate.of(2026, 5, 5)
        val windows = listOf(
            UsageWindow(
                start = Instant.parse("2026-05-05T09:00:00Z"),
                end = Instant.parse("2026-05-05T10:00:00Z"),
                appUsageMs = mapOf("chat" to 20L * 60_000L, "maps" to 10L * 60_000L)
            ),
            UsageWindow(
                start = Instant.parse("2026-05-05T10:00:00Z"),
                end = Instant.parse("2026-05-05T11:00:00Z"),
                appUsageMs = mapOf("chat" to 5L * 60_000L)
            )
        )

        val trend = builder.buildTrendFromHourlyWindows(
            day = day,
            hourlyWindows = windows,
            labelResolver = { it.replaceFirstChar(Char::uppercase) },
            colorResolver = { if (it == "chat") 0xFF0000 else 0x00FF00 }
        )

        assertEquals(30, trend.total.points.first { it.hour == 10 }.value)
        assertEquals(5, trend.total.points.first { it.hour == 11 }.value)
        assertTrue(trend.apps.any { it.label == "Chat" })
        assertEquals(
            25,
            trend.apps.first { it.label == "Chat" }.points
                .filter { it.hour in listOf(10, 11) }
                .sumOf { it.value }
        )
    }

    private fun usageWindow(
        start: Instant,
        vararg appUsage: Pair<String, Long>
    ): UsageWindow {
        return UsageWindow(
            start = start,
            end = start.plusSeconds(60),
            appUsageMs = mapOf(*appUsage)
        )
    }
}
