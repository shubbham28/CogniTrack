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
        val days = listOf("M", "T", "W", "T", "F", "S", "S")
        days.forEachIndexed { dayIndex, label ->
            repeat(12) { hour ->
                add(HeatmapCell(label, hour, ((dayIndex + hour) % 5) / 4f))
            }
        }
    },
    trends = listOf(
        TrendPoint("7d", 54),
        TrendPoint("30d", 61),
        TrendPoint("90d", 47)
    ),
    flow = listOf(
        AppFlowStep("Docs", 0, 1800),
        AppFlowStep("Chat", 1, 900),
        AppFlowStep("Maps", 2, 600),
        AppFlowStep("Mail", 3, 780)
    )
)
