package com.digitalwellbeing.ui

import com.digitalwellbeing.capture.AppFlowStep
import java.time.LocalDate

data class TrendPoint(
    val label: String,
    val value: Int
)

data class DashboardState(
    val day: LocalDate,
    val totalMinutes: Long,
    val pickups: Int,
    val switches: Int,
    val focusScore: Int,
    val distractionScore: Int,
    val cognitiveLoadScore: Int,
    val timeline: List<TimelineSlice>,
    val heatmap: List<HeatmapCell>,
    val trends: List<TrendPoint>,
    val flow: List<AppFlowStep>
)

data class TimelineSlice(
    val label: String,
    val minutes: Int,
    val colorHex: Long
)

data class HeatmapCell(
    val dayLabel: String,
    val hour: Int,
    val intensity: Float
)
