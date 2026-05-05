package com.digitalwellbeing.storage

import java.time.Instant
import java.time.temporal.ChronoUnit

class RetentionPolicy(
    private val retentionDays: Long = 90
) {
    fun cutoff(now: Instant): Instant = now.minus(retentionDays, ChronoUnit.DAYS)
}
