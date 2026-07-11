package com.example.escaperoomtimer.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import com.example.escaperoomtimer.alarm.ManagerGameEndAlarmController
import com.example.escaperoomtimer.manager.HintProgressManager
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.notification.TimerNotificationHelper
import com.example.escaperoomtimer.network.ManagerTcpServer
import com.example.escaperoomtimer.widget.RoomStatusWidgetProvider

class TimerForegroundService : Service() {
    private var heartbeatTicks = 0
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            TimerManager.tickAll()
            if (TimerManager.consumeNaturallyCompletedRoomIds().isNotEmpty()) {
                ManagerGameEndAlarmController.play(applicationContext)
            }
            updateNotification()
            heartbeatTicks++
            if (heartbeatTicks >= 5) {
                heartbeatTicks = 0
                ManagerTcpServer.heartbeat()
            }
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        TimerManager.initialize(applicationContext)
        HintProgressManager.start(applicationContext)
        ManagerTcpServer.start()
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
        ManagerGameEndAlarmController.stop()
        ManagerTcpServer.stop()
        HintProgressManager.stop()
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
        ManagerTcpServer.broadcastRooms(TimerManager.rooms)
    }
}
