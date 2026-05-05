package com.digitalwellbeing.analytics

import com.digitalwellbeing.capture.AppSession
import com.digitalwellbeing.capture.EventSource
import com.digitalwellbeing.capture.EventType
import com.digitalwellbeing.capture.RawEvent
import com.digitalwellbeing.capture.SessionConfidence
import java.time.Duration
import java.time.Instant

class SessionStitcher(
    private val duplicateThreshold: Duration = Duration.ofSeconds(15)
) {
    fun stitch(events: List<RawEvent>): List<AppSession> {
        if (events.isEmpty()) return emptyList()

        val sorted = events.sortedBy { it.timestamp }
        val sessions = mutableListOf<AppSession>()
        var screenWindowId = 0L
        var screenOn = false
        var currentPackage: String? = null
        var currentStart: Instant? = null

        fun closeCurrent(endTs: Instant, confidence: SessionConfidence) {
            val packageName = currentPackage ?: return
            val startTs = currentStart ?: return
            if (endTs.isBefore(startTs)) return
            sessions += AppSession(
                packageName = packageName,
                startTs = startTs,
                endTs = endTs,
                durationSec = Duration.between(startTs, endTs).seconds.coerceAtLeast(0),
                screenWindowId = screenWindowId,
                confidence = confidence
            )
            currentPackage = null
            currentStart = null
        }

        for (event in sorted) {
            when (event.eventType) {
                EventType.SCREEN_ON -> {
                    screenOn = true
                    screenWindowId += 1
                }

                EventType.SCREEN_OFF -> {
                    if (currentPackage != null) {
                        closeCurrent(event.timestamp, SessionConfidence.MEDIUM)
                    }
                    screenOn = false
                }

                EventType.APP_FOREGROUND -> {
                    val packageName = event.packageName ?: continue
                    if (!screenOn) {
                        screenOn = true
                        if (screenWindowId == 0L) screenWindowId = 1L
                    }

                    if (packageName == currentPackage && currentStart != null) {
                        val elapsed = Duration.between(currentStart, event.timestamp).abs()
                        if (elapsed <= duplicateThreshold) {
                            continue
                        }
                    }

                    if (currentPackage != null && currentPackage != packageName) {
                        closeCurrent(event.timestamp, SessionConfidence.HIGH)
                    }
                    currentPackage = packageName
                    currentStart = event.timestamp
                }

                EventType.APP_BACKGROUND -> {
                    if (currentPackage != null && event.packageName == currentPackage) {
                        closeCurrent(event.timestamp, SessionConfidence.HIGH)
                    }
                }

                EventType.UNLOCK,
                EventType.NOTIFICATION_POSTED,
                EventType.MULTITASKING_HINT -> Unit
            }
        }

        if (currentPackage != null && currentStart != null) {
            closeCurrent(sorted.last().timestamp, SessionConfidence.LOW)
        }

        return sessions.filter { it.durationSec >= 0 }
    }
}
