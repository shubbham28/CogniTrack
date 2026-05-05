package com.digitalwellbeing.app

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import com.digitalwellbeing.capture.Collector
import com.digitalwellbeing.capture.CollectorMode
import com.digitalwellbeing.capture.EventSource
import com.digitalwellbeing.capture.EventType
import com.digitalwellbeing.capture.RawEvent
import java.time.Instant

class UsageStatsCollector(
    private val usageStatsManager: UsageStatsManager
) : Collector {
    override val mode: CollectorMode = CollectorMode.DEFAULT

    override suspend fun collect(windowStart: Instant, windowEnd: Instant): List<RawEvent> {
        val events = usageStatsManager.queryEvents(
            windowStart.toEpochMilli(),
            windowEnd.toEpochMilli()
        )
        return events.toRawEvents()
    }

    private fun UsageEvents.toRawEvents(): List<RawEvent> {
        val event = UsageEvents.Event()
        val rawEvents = mutableListOf<RawEvent>()
        while (hasNextEvent()) {
            getNextEvent(event)
            val eventType = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> EventType.APP_FOREGROUND
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> EventType.APP_BACKGROUND
                UsageEvents.Event.USER_INTERACTION -> EventType.UNLOCK
                else -> null
            } ?: continue

            rawEvents += RawEvent(
                timestamp = Instant.ofEpochMilli(event.timeStamp),
                eventType = eventType,
                packageName = event.packageName,
                source = EventSource.USAGE_STATS
            )
        }
        return rawEvents
    }
}
