package com.example.escaperoomtimer.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.notification.TimerNotificationHelper
import com.example.escaperoomtimer.widget.RoomStatusWidgetProvider

class TimerForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            TimerManager.tickAll()
            updateNotification()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        TimerManager.initialize(applicationContext)
        TimerNotificationHelper.createChannel(this)
        startForeground(
            TimerNotificationHelper.NOTIFICATION_ID,
            TimerNotificationHelper.buildTimerNotification(this)
        )
        handler.post(ticker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        TimerManager.persistNow()
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification() {
        NotificationManagerCompat.from(this).notify(
            TimerNotificationHelper.NOTIFICATION_ID,
            TimerNotificationHelper.buildTimerNotification(this)
        )
        RoomStatusWidgetProvider.updateAll(this)
    }
}
