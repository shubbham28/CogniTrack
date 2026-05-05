package com.digitalwellbeing.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.digitalwellbeing.capture.EventSource
import com.digitalwellbeing.capture.EventType
import com.digitalwellbeing.capture.RawEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant

class CogniNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { ScreenEventStore(applicationContext) }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: return
        serviceScope.launch {
            repository.append(
                RawEvent(
                    timestamp = Instant.now(),
                    eventType = EventType.NOTIFICATION_POSTED,
                    packageName = packageName,
                    source = EventSource.NOTIFICATION_LISTENER
                )
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
