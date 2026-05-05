package com.digitalwellbeing.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DashboardStateTest {
    @Test
    fun `sample dashboard state exposes all three hero graph families`() {
        val state = sampleDashboardState()

        assertThat(state.timeline).isNotEmpty()
        assertThat(state.heatmap).isNotEmpty()
        assertThat(state.trends.total.points).hasSize(24)
        assertThat(state.trends.apps).isNotEmpty()
        assertThat(state.flow).isNotEmpty()
    }
}
