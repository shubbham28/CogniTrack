package com.digitalwellbeing.capture

import java.time.Instant

data class AccessibilitySignal(
    val timestamp: Instant,
    val packageName: String?,
    val eventType: EventType
)

class AdvancedCollector(
    private val signalProvider: suspend (Instant, Instant) -> List<AccessibilitySignal>
) : Collector {
    override val mode: CollectorMode = CollectorMode.ADVANCED

    override suspend fun collect(windowStart: Instant, windowEnd: Instant): List<RawEvent> {
        return signalProvider(windowStart, windowEnd).map { signal ->
            RawEvent(
                timestamp = signal.timestamp,
                eventType = signal.eventType,
                packageName = signal.packageName,
                source = EventSource.ACCESSIBILITY
            )
        }
    }
}
