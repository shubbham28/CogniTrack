package com.digitalwellbeing.analytics

import com.digitalwellbeing.capture.EventType
import com.digitalwellbeing.capture.RawEvent
import java.time.Duration
import java.time.Instant

class PickupCounter(
    private val collapseWindow: Duration = Duration.ofSeconds(15)
) {
    fun count(events: List<RawEvent>): Int {
        val unlocks = events
            .filter { it.eventType == EventType.UNLOCK }
            .sortedBy { it.timestamp }

        var pickups = 0
        var lastAccepted: Instant? = null
        for (event in unlocks) {
            val previous = lastAccepted
            if (previous == null || Duration.between(previous, event.timestamp).abs() > collapseWindow) {
                pickups += 1
                lastAccepted = event.timestamp
            }
        }
        return pickups
    }
}
