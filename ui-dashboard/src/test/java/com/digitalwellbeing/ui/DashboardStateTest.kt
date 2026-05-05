package com.digitalwellbeing.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DashboardStateTest {
    @Test
    fun `sample dashboard state exposes all three hero graph families`() {
        val state = sampleDashboardState()

        assertThat(state.timeline).isNotEmpty()
        assertThat(state.heatmap).isNotEmpty()
        assertThat(state.trends).hasSize(3)
        assertThat(state.flow).isNotEmpty()
    }
}
