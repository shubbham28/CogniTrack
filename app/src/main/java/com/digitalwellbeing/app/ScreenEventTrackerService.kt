package com.digitalwellbeing.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.digitalwellbeing.capture.EventSource
import com.digitalwellbeing.capture.EventType
import com.digitalwellbeing.capture.RawEvent
import com.digitalwellbeing.storage.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant

class ScreenEventTrackerService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { ScreenEventStore(applicationContext) }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val eventType = when (action) {
                Intent.ACTION_SCREEN_ON -> EventType.SCREEN_ON
                Intent.ACTION_SCREEN_OFF -> EventType.SCREEN_OFF
                Intent.ACTION_USER_PRESENT -> EventType.UNLOCK
                else -> return
            }

            serviceScope.launch {
                repository.append(
                    RawEvent(
                        timestamp = Instant.now(),
                        eventType = eventType,
                        packageName = null,
                        source = EventSource.SCREEN_EVENTS
                    )
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, trackingNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun trackingNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("CogniTrack tracking active")
            .setContentText("Recording screen and unlock events for precise digital sessions.")
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CogniTrack Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground tracking for screen on, screen off, and unlock events."
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "screen-event-tracker"
        private const val NOTIFICATION_ID = 1001
    }
}

class ScreenEventStore(
    context: Context
) {
    private val dao = AppModule.provideDao(context)

    suspend fun append(event: RawEvent) {
        dao.insertRawEvents(listOf(event.toEntity()))
    }
}
