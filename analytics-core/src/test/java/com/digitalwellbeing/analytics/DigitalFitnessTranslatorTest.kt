package com.digitalwellbeing.analytics

import com.digitalwellbeing.capture.AppSession
import com.digitalwellbeing.capture.SessionConfidence
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class DigitalFitnessTranslatorTest {
    private val translator = DigitalFitnessTranslator()

    @Test
    fun `maps app switches into a higher intensity score`() {
        val summary = translator.summarize(
            sessions = listOf(
                session("com.mail", 0, 600),
                session("com.chat", 600, 900),
                session("com.docs", 900, 1800)
            ),
            notificationEvents = 4,
            multitaskingEvents = 2
        )

        assertThat(summary.totalTimeMinutes).isEqualTo(30)
        assertThat(summary.intensity.switchesPerHour).isGreaterThan(3.0)
        assertThat(summary.intensity.score).isGreaterThan(0)
        assertThat(summary.route.map { it.packageName }).containsExactly("com.mail", "com.chat", "com.docs").inOrder()
    }

    @Test
    fun `raises cognitive load when notifications and multitasking spike`() {
        val summary = translator.summarize(
            sessions = listOf(session("com.focus", 0, 1800)),
            notificationEvents = 6,
            multitaskingEvents = 3
        )

        assertThat(summary.cognitiveLoad.score).isAtLeast(70)
    }

    private fun session(packageName: String, start: Long, end: Long): AppSession {
        val base = Instant.parse("2026-05-05T08:00:00Z")
        return AppSession(
            packageName = packageName,
            startTs = base.plusSeconds(start),
            endTs = base.plusSeconds(end),
            durationSec = end - start,
            screenWindowId = 1L,
            confidence = SessionConfidence.HIGH
        )
    }
}
