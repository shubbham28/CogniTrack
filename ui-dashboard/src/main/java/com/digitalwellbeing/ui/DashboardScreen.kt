package com.digitalwellbeing.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.max

private enum class DashboardTab(val label: String) {
    HOME("Today"),
    INSIGHTS("Insights"),
    BEHAVIOR("Behavior"),
    TRUST("Trust")
}

private data class MetricInsight(
    val title: String,
    val score: Int,
    val color: Color,
    val summary: String,
    val details: List<String>
)

@Composable
fun DashboardScreen(
    state: DashboardState,
    selectedHeatmapCell: HeatmapCell? = null,
    hourInsight: HourInsight,
    statusLine: String? = null,
    onRefresh: (() -> Unit)? = null,
    onSelectHeatmapCell: ((HeatmapCell) -> Unit)? = null,
    onShowCurrentHourInsight: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedInsight by remember { mutableStateOf<MetricInsight?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
            ) {
                DashboardTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {},
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = DashboardTab.entries[selectedTab],
                label = "dashboard-tab"
            ) { tab ->
                when (tab) {
                    DashboardTab.HOME -> HomeTab(state, statusLine, onRefresh) { selectedInsight = it }
                    DashboardTab.INSIGHTS -> InsightsTab(
                        state = state,
                        selectedCell = selectedHeatmapCell,
                        hourInsight = hourInsight,
                        onSelectCell = onSelectHeatmapCell,
                        onShowCurrentHourInsight = onShowCurrentHourInsight
                    )
                    DashboardTab.BEHAVIOR -> BehaviorTab(state) { selectedInsight = it }
                    DashboardTab.TRUST -> TrustTab()
                }
            }
        }
    }

    selectedInsight?.let { insight ->
        AlertDialog(
            onDismissRequest = { selectedInsight = null },
            confirmButton = {
                TextButton(onClick = { selectedInsight = null }) {
                    Text("Close")
                }
            },
            title = { Text(insight.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(insight.summary, style = MaterialTheme.typography.bodyLarge)
                    insight.details.forEach { line ->
                        Text("• $line", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
        )
    }
}

@Composable
private fun HomeTab(
    state: DashboardState,
    statusLine: String?,
    onRefresh: (() -> Unit)?,
    onOpenInsight: (MetricInsight) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = "Cognitive mileage",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${formatMinutesLong(state.totalMinutes.toInt())} active across ${state.flow.size} visible sessions",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (statusLine != null || onRefresh != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusLine.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (onRefresh != null) {
                        FilledTonalButton(onClick = onRefresh) {
                            Text("Refresh")
                        }
                    }
                }
            }
        }
        item { ScoreStrip(state, onOpenInsight) }
        item { TimelineRibbon(state.timeline) }
        item { HourlyTrendCard(state.trends) }
    }
}

@Composable
private fun InsightsTab(
    state: DashboardState,
    selectedCell: HeatmapCell?,
    hourInsight: HourInsight,
    onSelectCell: ((HeatmapCell) -> Unit)?,
    onShowCurrentHourInsight: (() -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Weekly heatmap", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Tap any hour block for its 60-minute breakdown, or jump back to the latest 60 minutes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            if (onShowCurrentHourInsight != null) {
                FilledTonalButton(onClick = onShowCurrentHourInsight) {
                    Text("Current 60 min")
                }
            }
        }
        item {
            WeeklyHeatmap(
                cells = state.heatmap,
                selectedCell = selectedCell,
                onSelectCell = { cell -> onSelectCell?.invoke(cell) }
            )
        }
        item { HeatmapDetailCard(selectedCell, hourInsight) }
        item { SixtyMinuteInsightCard(hourInsight) }
    }
}

@Composable
private fun BehaviorTab(
    state: DashboardState,
    onOpenInsight: (MetricInsight) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("App flow route", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Recent visible sessions, with launchers and CogniTrack removed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.flow.forEach { step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${step.sequenceIndex + 1}. ${step.packageName}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatDuration(step.durationSec),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        item { ScoreStrip(state, onOpenInsight) }
    }
}

@Composable
private fun TrustTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Trust center", style = MaterialTheme.typography.headlineMedium)
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Default mode uses UsageStats plus screen events for low-friction tracking.")
                Text("Advanced mode adds Accessibility-backed app-flow detail only after explicit opt-in.")
                Text("Raw sessions expire after 90 days. Aggregates stay for long-range habit history.")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScoreStrip(
    state: DashboardState,
    onOpenInsight: (MetricInsight) -> Unit
) {
    val focusColor = MaterialTheme.colorScheme.secondary
    val intensityColor = MaterialTheme.colorScheme.primary
    val loadColor = MaterialTheme.colorScheme.tertiary

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScoreCard(
            label = "Focus",
            score = state.focusScore,
            color = focusColor,
            modifier = Modifier.weight(1f),
            onClick = {
                onOpenInsight(
                    MetricInsight(
                        title = "Focus",
                        score = state.focusScore,
                        color = focusColor,
                        summary = "Focus falls when your day fractures into many short sessions and repeated switches.",
                        details = listOf(
                            "${state.switches} app switches detected today.",
                            "${formatMinutesLong(state.totalMinutes.toInt())} of visible app usage tracked.",
                            "${state.pickups} pickup moments fed into the daily summary."
                        )
                    )
                )
            }
        )
        ScoreCard(
            label = "Intensity",
            score = state.distractionScore,
            color = intensityColor,
            modifier = Modifier.weight(1f),
            onClick = {
                onOpenInsight(
                    MetricInsight(
                        title = "Intensity",
                        score = state.distractionScore,
                        color = intensityColor,
                        summary = "Intensity is your digital pace: switching frequency, interruptions, and short bursts of context change.",
                        details = listOf(
                            "${state.switches} route transitions are currently contributing to intensity.",
                            "Higher short-session density increases this score.",
                            "Use the hourly trend to see when pace spikes."
                        )
                    )
                )
            }
        )
        ScoreCard(
            label = "Load",
            score = state.cognitiveLoadScore,
            color = loadColor,
            modifier = Modifier.weight(1f),
            onClick = {
                onOpenInsight(
                    MetricInsight(
                        title = "Load",
                        score = state.cognitiveLoadScore,
                        color = loadColor,
                        summary = "Load estimates cognitive pressure from notifications and fragmented multitasking.",
                        details = listOf(
                            "Notification listener events contribute directly when enabled.",
                            "Very short sessions are treated as multitasking hints.",
                            "The heatmap helps locate the heaviest hours."
                        )
                    )
                )
            }
        )
    }
}

@Composable
private fun ScoreCard(
    label: String,
    score: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Tap for what drives this score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(color, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = score.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineRibbon(slices: List<TimelineSlice>) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Daily route", style = MaterialTheme.typography.titleLarge)
            Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                val totalMinutes = slices.sumOf { it.minutes }.coerceAtLeast(1)
                var cursor = 0f
                slices.forEach { slice ->
                    val width = size.width * (slice.minutes / totalMinutes.toFloat())
                    drawRoundRect(
                        color = Color(slice.colorHex),
                        topLeft = Offset(cursor, 0f),
                        size = Size(width, size.height),
                        cornerRadius = CornerRadius(22f, 22f)
                    )
                    cursor += width
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                slices.forEach { slice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(slice.colorHex), RoundedCornerShape(99.dp))
                        )
                        Text(
                            "${slice.label} ${formatMinutesLong(slice.minutes)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyHeatmap(
    cells: List<HeatmapCell>,
    selectedCell: HeatmapCell?,
    onSelectCell: (HeatmapCell) -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Load pattern", style = MaterialTheme.typography.titleLarge)
            cells
                .groupBy { it.dayLabel }
                .forEach { (day, values) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(day, modifier = Modifier.width(28.dp))
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            values.sortedBy { it.hour }.forEach { cell ->
                                val isSelected = selectedCell?.dayLabel == cell.dayLabel && selectedCell.hour == cell.hour
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(20.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f + cell.intensity * 0.82f),
                                            shape = RoundedCornerShape(7.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                            shape = RoundedCornerShape(7.dp)
                                        )
                                        .clickable { onSelectCell(cell) }
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun HeatmapDetailCard(
    cell: HeatmapCell?,
    hourInsight: HourInsight
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(hourInsight.title, style = MaterialTheme.typography.titleLarge)
            Text(
                hourInsight.subtitle,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                if (cell != null) {
                    if (cell.minutes > 0) "${formatMinutesLong(cell.minutes)} visible in selected hour." else "No visible app usage in this hour."
                } else {
                    "Showing the latest rolling 60-minute window."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hourInsight.pieBreakdown.isNotEmpty()) {
                hourInsight.pieBreakdown.forEach { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(app.colorHex), RoundedCornerShape(99.dp))
                            )
                            Text(app.label, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(formatMinutesLong(app.minutes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SixtyMinuteInsightCard(hourInsight: HourInsight) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("60-minute trend", style = MaterialTheme.typography.titleLarge)
            Text(
                "Minute-by-minute foreground activity for the selected hour, with a breakdown of app time versus idle time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MinuteTrendStrip(hourInsight)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Visible ${formatMinutesLong(hourInsight.totalVisibleMinutes)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Idle ${formatMinutesLong(hourInsight.idleMinutes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HourPieChart(hourInsight.pieBreakdown)
        }
    }
}

@Composable
private fun MinuteTrendStrip(hourInsight: HourInsight) {
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        repeat(4) { index ->
            val y = size.height * (index / 3f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2f
            )
        }

        val stepX = size.width / hourInsight.minuteTrend.size.coerceAtLeast(1)
        hourInsight.minuteTrend.forEachIndexed { index, point ->
            val barHeight = size.height * point.activeFraction.coerceIn(0.04f, 1f)
            drawRoundRect(
                color = Color(point.colorHex),
                topLeft = Offset(index * stepX + 1f, size.height - barHeight),
                size = Size((stepX - 2f).coerceAtLeast(2f), barHeight),
                cornerRadius = CornerRadius(5f, 5f)
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("00", "15", "30", "45", "59").forEach { label ->
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HourPieChart(pieBreakdown: List<AppUsageBreakdown>) {
    val totalMinutes = pieBreakdown.sumOf { it.minutes }.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(148.dp)) {
            var startAngle = -90f
            pieBreakdown.forEach { slice ->
                val sweep = 360f * (slice.minutes / totalMinutes.toFloat())
                drawArc(
                    color = Color(slice.colorHex),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 42f, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            pieBreakdown.forEach { slice ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(slice.colorHex), RoundedCornerShape(99.dp))
                        )
                        Text(slice.label, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(formatMinutesLong(slice.minutes), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HourlyTrendCard(trendChart: TrendChart) {
    var selectedPackage by remember(trendChart) { mutableStateOf<String?>(null) }
    val selectedSeries = trendChart.apps.firstOrNull { it.packageName == selectedPackage }

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Hourly trend", style = MaterialTheme.typography.titleLarge)
            Text(
                "Hours on the x-axis, minutes used on the y-axis. Total stays visible while you filter by app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPackage == null,
                    onClick = { selectedPackage = null },
                    label = { Text("Total") }
                )
                trendChart.apps.forEach { series ->
                    FilterChip(
                        selected = selectedPackage == series.packageName,
                        onClick = { selectedPackage = series.packageName },
                        label = { Text(series.label) }
                    )
                }
            }
            HourlyTrendChart(
                totalSeries = trendChart.total,
                selectedSeries = selectedSeries
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total today ${formatMinutesLong(trendChart.total.points.sumOf { it.value })}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = selectedSeries?.let { "${it.label} ${formatMinutesLong(it.points.sumOf { point -> point.value })}" } ?: "All apps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HourlyTrendChart(
    totalSeries: TrendSeries,
    selectedSeries: TrendSeries?
) {
    val maxValue = max(
        totalSeries.points.maxOfOrNull { it.value } ?: 0,
        selectedSeries?.points?.maxOfOrNull { it.value } ?: 0
    ).coerceAtLeast(1)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${maxValue}m", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${maxValue / 2}m", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("0m", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val horizontalGrid = 4
            repeat(horizontalGrid + 1) { index ->
                val y = size.height * (index / horizontalGrid.toFloat())
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f
                )
            }

            fun pointsFor(series: TrendSeries): List<Offset> {
                val stepX = size.width / (series.points.size - 1).coerceAtLeast(1)
                return series.points.mapIndexed { index, point ->
                    Offset(
                        x = index * stepX,
                        y = size.height - (point.value / maxValue.toFloat() * size.height)
                    )
                }
            }

            val totalPoints = pointsFor(totalSeries)
            drawSeries(totalPoints, Color(totalSeries.colorHex).copy(alpha = if (selectedSeries == null) 0.95f else 0.35f), 8f)

            selectedSeries?.let { series ->
                drawSeries(pointsFor(series), Color(series.colorHex), 10f)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("00", "06", "12", "18", "23").forEach { label ->
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSeries(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float
) {
    for (index in 0 until points.lastIndex) {
        drawLine(
            color = color,
            start = points[index],
            end = points[index + 1],
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.cornerPathEffect(20f)
        )
    }
    points.forEach { point ->
        drawCircle(color = color, radius = strokeWidth, center = point, style = Stroke(width = 2f))
        drawCircle(color = color, radius = strokeWidth / 2f, center = point)
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
        ),
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content
        )
    }
}

private fun formatDuration(durationSec: Long): String {
    val hours = durationSec / 3600
    val minutes = (durationSec % 3600) / 60
    val seconds = durationSec % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

private fun formatMinutesLong(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours > 0 && remainder > 0 -> "${hours}h ${remainder}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
