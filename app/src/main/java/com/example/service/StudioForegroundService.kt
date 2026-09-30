package com.example.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StudioForegroundService : Service() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESC = "extra_desc"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        _isServiceRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == NotificationHelper.ACTION_STOP_SESSION) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            _isServiceRunning.value = false
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Live Studio Session"
        val desc = intent?.getStringExtra(EXTRA_DESC) ?: "Scene recording or live streaming active"

        val notification = NotificationHelper.buildNotification(this, title, desc)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val types = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            runCatching {
                startForeground(NotificationHelper.NOTIFICATION_ID, notification, types)
            }.onFailure {
                // Fallback if media projection token isn't attached yet
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
