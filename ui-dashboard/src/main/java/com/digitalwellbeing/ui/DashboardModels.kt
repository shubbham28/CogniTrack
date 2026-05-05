package com.digitalwellbeing.ui

import com.digitalwellbeing.capture.AppFlowStep
import java.time.LocalDate

data class MinuteTrendPoint(
    val minuteIndex: Int,
    val activeFraction: Float,
    val colorHex: Long,
    val label: String
)

data class TrendPoint(
    val hour: Int,
    val label: String,
    val value: Int,
    val formattedValue: String
)

data class TrendSeries(
    val packageName: String?,
    val label: String,
    val colorHex: Long,
    val points: List<TrendPoint>
)

data class TrendChart(
    val total: TrendSeries,
    val apps: List<TrendSeries>
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
    val trends: TrendChart,
    val flow: List<AppFlowStep>
)

data class HourInsight(
    val title: String,
    val subtitle: String,
    val totalVisibleMinutes: Int,
    val idleMinutes: Int,
    val minuteTrend: List<MinuteTrendPoint>,
    val pieBreakdown: List<AppUsageBreakdown>
)

data class TimelineSlice(
    val label: String,
    val minutes: Int,
    val colorHex: Long
)

data class HeatmapCell(
    val date: LocalDate,
    val dayLabel: String,
    val dateLabel: String,
    val hour: Int,
    val intensity: Float,
    val minutes: Int,
    val apps: List<AppUsageBreakdown>
)

data class AppUsageBreakdown(
    val label: String,
    val minutes: Int,
    val colorHex: Long
)
