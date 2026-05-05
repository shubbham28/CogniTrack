package com.digitalwellbeing.capture

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class CollectorOrchestratorTest {
    private val defaultCollector = FakeCollector(CollectorMode.DEFAULT)
    private val advancedCollector = FakeCollector(CollectorMode.ADVANCED)
    private val orchestrator = CollectorOrchestrator(defaultCollector, advancedCollector)

    @Test
    fun `defaults to usage stats collector when advanced mode is disabled`() {
        val collector = orchestrator.activeCollector(
            PermissionSnapshot(
                hasUsageAccess = true,
                hasNotificationAccess = true,
                hasAccessibilityAccess = false,
                advancedModeEnabled = false
            )
        )

        assertThat(collector.mode).isEqualTo(CollectorMode.DEFAULT)
    }

    @Test
    fun `keeps default collector when advanced mode lacks accessibility permission`() {
        val collector = orchestrator.activeCollector(
            PermissionSnapshot(
                hasUsageAccess = true,
                hasNotificationAccess = true,
                hasAccessibilityAccess = false,
                advancedModeEnabled = true
            )
        )

        assertThat(collector.mode).isEqualTo(CollectorMode.DEFAULT)
    }

    @Test
    fun `uses advanced collector when every advanced permission is granted`() {
        val collector = orchestrator.activeCollector(
            PermissionSnapshot(
                hasUsageAccess = true,
                hasNotificationAccess = true,
                hasAccessibilityAccess = true,
                advancedModeEnabled = true
            )
        )

        assertThat(collector.mode).isEqualTo(CollectorMode.ADVANCED)
    }

    private class FakeCollector(
        override val mode: CollectorMode
    ) : Collector {
        override suspend fun collect(windowStart: Instant, windowEnd: Instant): List<RawEvent> = emptyList()
    }
}
