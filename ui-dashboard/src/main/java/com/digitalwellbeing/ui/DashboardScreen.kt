package com.digitalwellbeing.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private enum class DashboardTab(val label: String) {
    HOME("Today"),
    INSIGHTS("Insights"),
    BEHAVIOR("Behavior"),
    TRUST("Trust")
}

@Composable
fun DashboardScreen(
    state: DashboardState,
    statusLine: String? = null,
    onRefresh: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
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
        AnimatedContent(
            targetState = DashboardTab.entries[selectedTab],
            modifier = Modifier.padding(padding),
            label = "dashboard-tab"
        ) { tab ->
            when (tab) {
                DashboardTab.HOME -> HomeTab(state, statusLine, onRefresh)
                DashboardTab.INSIGHTS -> InsightsTab(state)
                DashboardTab.BEHAVIOR -> BehaviorTab(state)
                DashboardTab.TRUST -> TrustTab()
            }
        }
    }
}

@Composable
private fun HomeTab(
    state: DashboardState,
    statusLine: String?,
    onRefresh: (() -> Unit)?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Cognitive mileage",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${state.totalMinutes} min active across ${state.timeline.size} sessions",
                style = MaterialTheme.typography.bodyLarge
            )
            if (statusLine != null || onRefresh != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusLine ?: "",
                        style = MaterialTheme.typography.bodyMedium,
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
        item { ScoreStrip(state) }
        item { TimelineRibbon(state.timeline) }
        item { TrendDeck(state.trends) }
    }
}

@Composable
private fun InsightsTab(state: DashboardState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Weekly heatmap", style = MaterialTheme.typography.headlineMedium)
        }
        item { WeeklyHeatmap(state.heatmap) }
        item { TrendDeck(state.trends) }
    }
}

@Composable
private fun BehaviorTab(state: DashboardState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("App flow route", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.flow.forEach { step ->
                        Text(
                            text = "${step.sequenceIndex + 1}. ${step.packageName} · ${step.durationSec / 60} min",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        item { ScoreStrip(state) }
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
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 4.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Default mode uses UsageStats plus screen events for low-friction tracking.")
                Text("Advanced mode adds Accessibility-backed app-flow detail only after explicit opt-in.")
                Text("Raw sessions expire after 90 days. Aggregates stay for long-range habit history.")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScoreStrip(state: DashboardState) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScoreCard("Focus", state.focusScore, MaterialTheme.colorScheme.secondary)
        ScoreCard("Intensity", state.distractionScore, MaterialTheme.colorScheme.primary)
        ScoreCard("Load", state.cognitiveLoadScore, MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun ScoreCard(label: String, score: Int, color: Color) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleLarge)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(color, RoundedCornerShape(16.dp)),
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

@Composable
fun TimelineRibbon(slices: List<TimelineSlice>) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Daily route", style = MaterialTheme.typography.titleLarge)
            Canvas(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                val totalMinutes = slices.sumOf { it.minutes }.coerceAtLeast(1)
                var cursor = 0f
                slices.forEach { slice ->
                    val width = size.width * (slice.minutes / totalMinutes.toFloat())
                    drawRoundRect(
                        color = Color(slice.colorHex),
                        topLeft = Offset(cursor, 0f),
                        size = androidx.compose.ui.geometry.Size(width, size.height),
                        cornerRadius = CornerRadius(18f, 18f)
                    )
                    cursor += width
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                slices.take(3).forEach { slice ->
                    Text("${slice.label} ${slice.minutes}m", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun WeeklyHeatmap(cells: List<HeatmapCell>) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Load pattern", style = MaterialTheme.typography.titleLarge)
            val grouped = cells.groupBy { it.dayLabel }
            grouped.forEach { (day, values) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(day, modifier = Modifier.weight(0.22f))
                    Row(modifier = Modifier.weight(0.78f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        values.sortedBy { it.hour }.forEach { cell ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(18.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f + cell.intensity * 0.82f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendDeck(trends: List<TrendPoint>) {
    val trendColor = MaterialTheme.colorScheme.tertiary
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Trend cards", style = MaterialTheme.typography.titleLarge)
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                if (trends.isEmpty()) return@Canvas
                val max = trends.maxOf { it.value }.coerceAtLeast(1)
                val stepX = size.width / (trends.size - 1).coerceAtLeast(1)
                val points = trends.mapIndexed { index, point ->
                    Offset(
                        x = index * stepX,
                        y = size.height - (point.value / max.toFloat() * size.height)
                    )
                }
                for (index in 0 until points.lastIndex) {
                    drawLine(
                        color = trendColor,
                        start = points[index],
                        end = points[index + 1],
                        strokeWidth = 8f,
                        pathEffect = PathEffect.cornerPathEffect(18f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                trends.forEach { point ->
                    Text("${point.label} ${point.value}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
