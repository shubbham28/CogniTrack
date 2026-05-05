package com.digitalwellbeing.capture

class PermissionStateEvaluator {
    fun evaluate(snapshot: PermissionSnapshot): PermissionState {
        if (!snapshot.hasUsageAccess) {
            return PermissionState.NEEDS_USAGE_ACCESS
        }
        if (!snapshot.hasNotificationAccess) {
            return PermissionState.NEEDS_NOTIFICATION_ACCESS
        }
        if (snapshot.advancedModeEnabled && !snapshot.hasAccessibilityAccess) {
            return PermissionState.NEEDS_ACCESSIBILITY_ACCESS
        }
        return PermissionState.READY
    }
}
