package com.digitalwellbeing.app

import android.app.usage.UsageStatsManager
import java.time.Instant

class UsageSummaryReader(
    private val usageStatsManager: UsageStatsManager
) {
    fun totalMinutesBetween(start: Instant, end: Instant): Int {
        val aggregated = usageStatsManager.queryAndAggregateUsageStats(
            start.toEpochMilli(),
            end.toEpochMilli()
        )
        return (aggregated.values.sumOf { it.totalTimeInForeground } / 60000L).toInt()
    }
}
