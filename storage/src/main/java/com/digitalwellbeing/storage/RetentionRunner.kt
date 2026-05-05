package com.digitalwellbeing.storage

import java.time.Instant

interface RetentionStore {
    suspend fun purgeRawEventsBefore(cutoff: Instant)
    suspend fun purgeSessionsBefore(cutoff: Instant)
}

class RetentionRunner(
    private val policy: RetentionPolicy,
    private val store: RetentionStore
) {
    suspend fun run(now: Instant) {
        val cutoff = policy.cutoff(now)
        store.purgeRawEventsBefore(cutoff)
        store.purgeSessionsBefore(cutoff)
    }
}
