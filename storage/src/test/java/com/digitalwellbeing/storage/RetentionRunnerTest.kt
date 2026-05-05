package com.digitalwellbeing.storage

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class RetentionRunnerTest {
    @Test
    fun `purges raw events and sessions older than ninety days while preserving aggregates`() = runTest {
        val store = FakeStore()
        val now = Instant.parse("2026-05-05T08:00:00Z")

        RetentionRunner(RetentionPolicy(90), store).run(now)

        assertThat(store.rawCutoff).isEqualTo(Instant.parse("2026-02-04T08:00:00Z"))
        assertThat(store.sessionCutoff).isEqualTo(Instant.parse("2026-02-04T08:00:00Z"))
    }

    private class FakeStore : RetentionStore {
        var rawCutoff: Instant? = null
        var sessionCutoff: Instant? = null

        override suspend fun purgeRawEventsBefore(cutoff: Instant) {
            rawCutoff = cutoff
        }

        override suspend fun purgeSessionsBefore(cutoff: Instant) {
            sessionCutoff = cutoff
        }
    }
}
