package com.digitalwellbeing.capture

import java.time.Instant

interface Collector {
    val mode: CollectorMode

    suspend fun collect(windowStart: Instant, windowEnd: Instant): List<RawEvent>
}
