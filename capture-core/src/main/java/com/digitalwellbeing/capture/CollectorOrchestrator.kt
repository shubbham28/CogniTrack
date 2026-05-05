package com.digitalwellbeing.capture

class CollectorOrchestrator(
    private val defaultCollector: Collector,
    private val advancedCollector: Collector,
    private val evaluator: PermissionStateEvaluator = PermissionStateEvaluator()
) {
    fun activeCollector(snapshot: PermissionSnapshot): Collector {
        val permissionState = evaluator.evaluate(snapshot)
        return if (snapshot.advancedModeEnabled &&
            permissionState == PermissionState.READY &&
            snapshot.hasAccessibilityAccess
        ) {
            advancedCollector
        } else {
            defaultCollector
        }
    }
}
