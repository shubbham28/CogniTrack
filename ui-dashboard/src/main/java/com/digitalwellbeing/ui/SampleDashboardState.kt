package com.digitalwellbeing.ui

import com.digitalwellbeing.capture.AppFlowStep
import java.time.LocalDate

fun sampleDashboardState(): DashboardState = DashboardState(
    day = LocalDate.now(),
    totalMinutes = 286,
    pickups = 41,
    switches = 67,
    focusScore = 62,
    distractionScore = 58,
    cognitiveLoadScore = 44,
    timeline = listOf(
        TimelineSlice("Docs", 88, 0xFFD64C2F),
        TimelineSlice("Chat", 46, 0xFF2A3441),
        TimelineSlice("Maps", 34, 0xFF9BC53D),
        TimelineSlice("Mail", 52, 0xFF6B7A8F),
        TimelineSlice("Video", 66, 0xFFDD8C6F)
    ),
    heatmap = buildList {
        val today = LocalDate.now()
        val days = listOf("Wed", "Thu", "Fri", "Sat", "Sun", "Mon", "Tue")
        days.forEachIndexed { dayIndex, label ->
            val date = today.minusDays((days.lastIndex - dayIndex).toLong())
            repeat(24) { hour ->
                add(
                    HeatmapCell(
                        date = date,
                        dayLabel = label,
                        dateLabel = date.toString(),
                        hour = hour,
                        intensity = ((dayIndex + hour) % 5) / 4f,
                        minutes = ((dayIndex + hour) % 4) * 12,
                        apps = listOf(
                            AppUsageBreakdown("Docs", 18, 0xFFD64C2F),
                            AppUsageBreakdown("Chat", 10, 0xFF2A3441)
                        )
                    )
                )
            }
        }
    },
    trends = TrendChart(
        total = TrendSeries(
            packageName = null,
            label = "Total",
            colorHex = 0xFFD64C2F,
            points = (0..23).map { hour ->
                val minutes = when {
                    hour in 8..10 -> 22
                    hour in 11..14 -> 36
                    hour in 19..22 -> 28
                    else -> 6
                }
                TrendPoint(hour, hour.toString().padStart(2, '0'), minutes, "${minutes}m")
            }
        ),
        apps = listOf(
            TrendSeries(
                packageName = "docs",
                label = "Docs",
                colorHex = 0xFF9BC53D,
                points = (0..23).map { hour ->
                    val minutes = if (hour in 9..12) 18 else 0
                    TrendPoint(hour, hour.toString().padStart(2, '0'), minutes, "${minutes}m")
                }
            ),
            TrendSeries(
                packageName = "chat",
                label = "Chat",
                colorHex = 0xFF6B7A8F,
                points = (0..23).map { hour ->
                    val minutes = if (hour in 19..22) 14 else 2
                    TrendPoint(hour, hour.toString().padStart(2, '0'), minutes, "${minutes}m")
                }
            )
        )
    ),
    flow = listOf(
        AppFlowStep("Docs", 0, 1800),
        AppFlowStep("Chat", 1, 900),
        AppFlowStep("Maps", 2, 600),
        AppFlowStep("Mail", 3, 780)
    )
)
