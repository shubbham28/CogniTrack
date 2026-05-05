package com.digitalwellbeing.analytics

import com.digitalwellbeing.capture.EventSource
import com.digitalwellbeing.capture.EventType
import com.digitalwellbeing.capture.RawEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class PickupCounterTest {
    private val counter = PickupCounter()
    private val base = Instant.parse("2026-05-05T08:00:00Z")

    @Test
    fun `collapses duplicate unlocks from multiple sources in the same burst`() {
        val pickups = counter.count(
            listOf(
                RawEvent(base, EventType.UNLOCK, null, EventSource.USAGE_STATS),
                RawEvent(base.plusSeconds(5), EventType.UNLOCK, null, EventSource.SCREEN_EVENTS),
                RawEvent(base.plusSeconds(20), EventType.UNLOCK, null, EventSource.SCREEN_EVENTS)
            )
        )

        assertThat(pickups).isEqualTo(2)
    }

    @Test
    fun `ignores non unlock events`() {
        val pickups = counter.count(
            listOf(
                RawEvent(base, EventType.SCREEN_ON, null, EventSource.SCREEN_EVENTS),
                RawEvent(base.plusSeconds(3), EventType.APP_FOREGROUND, "com.mail", EventSource.USAGE_STATS)
            )
        )

        assertThat(pickups).isEqualTo(0)
    }
}
