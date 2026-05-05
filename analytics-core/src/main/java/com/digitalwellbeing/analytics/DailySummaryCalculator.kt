package com.digitalwellbeing.analytics

import com.digitalwellbeing.capture.AppSession
import com.digitalwellbeing.capture.DailySummary
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class DailySummaryCalculator(
    private val zoneId: ZoneId
) {
    fun calculate(
        day: LocalDate,
        sessions: List<AppSession>,
        pickups: Int,
        notifications: Int,
        multitaskingEvents: Int
    ): DailySummary {
        val sameDaySessions = sessions.filter {
            it.startTs.atZone(zoneId).toLocalDate() == day
        }
        val translator = DigitalFitnessTranslator().summarize(
            sessions = sameDaySessions,
            notificationEvents = notifications,
            multitaskingEvents = multitaskingEvents
        )
        val focusScore = (100 - translator.intensity.score * 0.55).roundToInt().coerceIn(0, 100)
        val distractionScore = translator.intensity.score

        return DailySummary(
            day = day,
            totalScreenTimeMinutes = translator.totalTimeMinutes,
            totalPickups = pickups,
            totalAppSwitches = translator.route.zipWithNext().count { it.first.packageName != it.second.packageName },
            focusScore = focusScore,
            distractionScore = distractionScore,
            cognitiveLoadScore = translator.cognitiveLoad.score
        )
    }
}
