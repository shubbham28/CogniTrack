package com.digitalwellbeing.capture

import java.time.Instant
import java.time.LocalDate

enum class EventType {
    SCREEN_ON,
    SCREEN_OFF,
    APP_FOREGROUND,
    APP_BACKGROUND,
    UNLOCK,
    NOTIFICATION_POSTED,
    MULTITASKING_HINT
}

enum class EventSource {
    USAGE_STATS,
    SCREEN_EVENTS,
    ACCESSIBILITY,
    SYNTHETIC
}

enum class SessionConfidence {
    HIGH,
    MEDIUM,
    LOW
}

data class RawEvent(
    val timestamp: Instant,
    val eventType: EventType,
    val packageName: String? = null,
    val source: EventSource
)

data class AppSession(
    val packageName: String,
    val startTs: Instant,
    val endTs: Instant,
    val durationSec: Long,
    val screenWindowId: Long,
    val confidence: SessionConfidence
)

data class DailySummary(
    val day: LocalDate,
    val totalScreenTimeMinutes: Long,
    val totalPickups: Int,
    val totalAppSwitches: Int,
    val focusScore: Int,
    val distractionScore: Int,
    val cognitiveLoadScore: Int
)

data class AppFlowStep(
    val packageName: String,
    val sequenceIndex: Int,
    val durationSec: Long
)

enum class CollectorMode {
    DEFAULT,
    ADVANCED
}

enum class PermissionState {
    READY,
    NEEDS_USAGE_ACCESS,
    NEEDS_NOTIFICATION_ACCESS,
    NEEDS_ACCESSIBILITY_ACCESS
}

data class PermissionSnapshot(
    val hasUsageAccess: Boolean,
    val hasNotificationAccess: Boolean,
    val hasAccessibilityAccess: Boolean,
    val advancedModeEnabled: Boolean
)
