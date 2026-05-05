package com.digitalwellbeing.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        if (PermissionGateway(context).hasUsageAccess()) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ScreenEventTrackerService::class.java)
            )
        }
    }
}
