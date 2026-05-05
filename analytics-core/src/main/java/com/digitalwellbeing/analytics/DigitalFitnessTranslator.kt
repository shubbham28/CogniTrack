package com.digitalwellbeing.analytics

import com.digitalwellbeing.capture.AppFlowStep
import com.digitalwellbeing.capture.AppSession
import java.time.Duration
import kotlin.math.roundToInt

data class IntensityScore(
    val switchesPerHour: Double,
    val interruptionsPerHour: Double,
    val score: Int
)

data class CognitiveLoadScore(
    val notificationBursts: Int,
    val multitaskingMoments: Int,
    val score: Int
)

data class DigitalWorkoutSummary(
    val appSessions: List<AppSession>,
    val totalTimeMinutes: Long,
    val intensity: IntensityScore,
    val cognitiveLoad: CognitiveLoadScore,
    val route: List<AppFlowStep>
)

class DigitalFitnessTranslator {
    fun summarize(
        sessions: List<AppSession>,
        notificationEvents: Int,
        multitaskingEvents: Int
    ): DigitalWorkoutSummary {
        val totalSeconds = sessions.sumOf { it.durationSec }
        val hours = (totalSeconds / 3600.0).coerceAtLeast(1.0 / 60.0)
        val switches = sessions.zipWithNext().count { it.first.packageName != it.second.packageName }
        val interruptions = switches + notificationEvents

        val intensity = IntensityScore(
            switchesPerHour = switches / hours,
            interruptionsPerHour = interruptions / hours,
            score = ((switches / hours) * 12 + (interruptions / hours) * 4).roundToInt().coerceIn(0, 100)
        )
        val cognitiveLoad = CognitiveLoadScore(
            notificationBursts = notificationEvents,
            multitaskingMoments = multitaskingEvents,
            score = (notificationEvents * 8 + multitaskingEvents * 10).coerceIn(0, 100)
        )
        val route = sessions.mapIndexed { index, session ->
            AppFlowStep(
                packageName = session.packageName,
                sequenceIndex = index,
                durationSec = session.durationSec
            )
        }

        return DigitalWorkoutSummary(
            appSessions = sessions,
            totalTimeMinutes = Duration.ofSeconds(totalSeconds).toMinutes(),
            intensity = intensity,
            cognitiveLoad = cognitiveLoad,
            route = route
        )
    }
}
