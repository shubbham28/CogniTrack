package com.digitalwellbeing.analytics

import com.digitalwellbeing.capture.EventSource
import com.digitalwellbeing.capture.EventType
import com.digitalwellbeing.capture.RawEvent
import com.digitalwellbeing.capture.SessionConfidence
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class SessionStitcherTest {
    private val stitcher = SessionStitcher()
    private val base = Instant.parse("2026-05-05T08:00:00Z")

    @Test
    fun `closes active session when screen turns off without background event`() {
        val sessions = stitcher.stitch(
            listOf(
                event(0, EventType.SCREEN_ON),
                event(10, EventType.APP_FOREGROUND, "com.chat"),
                event(310, EventType.SCREEN_OFF)
            )
        )

        assertThat(sessions).hasSize(1)
        assertThat(sessions.first().durationSec).isEqualTo(300)
        assertThat(sessions.first().confidence).isEqualTo(SessionConfidence.MEDIUM)
    }

    @Test
    fun `collapses duplicate foreground bursts for the same app`() {
        val sessions = stitcher.stitch(
            listOf(
                event(0, EventType.SCREEN_ON),
                event(5, EventType.APP_FOREGROUND, "com.reader"),
                event(10, EventType.APP_FOREGROUND, "com.reader"),
                event(120, EventType.APP_BACKGROUND, "com.reader")
            )
        )

        assertThat(sessions).hasSize(1)
        assertThat(sessions.first().durationSec).isEqualTo(115)
    }

    @Test
    fun `splits sessions across midnight by raw event boundaries`() {
        val midnight = Instant.parse("2026-05-05T23:59:30Z")
        val sessions = stitcher.stitch(
            listOf(
                RawEvent(midnight, EventType.SCREEN_ON, null, EventSource.SCREEN_EVENTS),
                RawEvent(midnight.plusSeconds(10), EventType.APP_FOREGROUND, "com.maps", EventSource.USAGE_STATS),
                RawEvent(midnight.plusSeconds(70), EventType.APP_BACKGROUND, "com.maps", EventSource.USAGE_STATS)
            )
        )

        assertThat(sessions).hasSize(1)
        assertThat(sessions.first().durationSec).isEqualTo(60)
    }

    private fun event(offsetSeconds: Long, type: EventType, pkg: String? = null): RawEvent {
        return RawEvent(
            timestamp = base.plusSeconds(offsetSeconds),
            eventType = type,
            packageName = pkg,
            source = if (type == EventType.SCREEN_ON || type == EventType.SCREEN_OFF) {
                EventSource.SCREEN_EVENTS
            } else {
                EventSource.USAGE_STATS
            }
        )
    }
}
