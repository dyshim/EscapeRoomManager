package com.example.escaperoomtimer.service

import android.app.Service
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.PowerManager
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import com.example.escaperoomtimer.alarm.ManagerGameEndAlarmController
import com.example.escaperoomtimer.manager.HintProgressManager
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.notification.TimerNotificationHelper
import com.example.escaperoomtimer.network.ManagerTcpServer
import com.example.escaperoomtimer.widget.RoomStatusWidgetProvider
import com.example.escaperoomtimer.web.ManagerWebServer

class TimerForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
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
                ManagerWebServer.ensureStarted(applicationContext)
                heartbeatTicks = 0
                ManagerTcpServer.heartbeat()
            }
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        TimerManager.initialize(applicationContext)
        acquireRuntimeLocks()
        HintProgressManager.start(applicationContext)
        ManagerTcpServer.start()
        ManagerWebServer.start(applicationContext)
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
        ManagerWebServer.stop()
        HintProgressManager.stop()
        handler.removeCallbacks(ticker)
        releaseRuntimeLocks()
        super.onDestroy()
    }

    private fun acquireRuntimeLocks() {
        runCatching {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "EscapeRoomTimer:ManagerRuntime"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        }

        runCatching {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "EscapeRoomTimer:ManagerWifi"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releaseRuntimeLocks() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        runCatching { if (wifiLock?.isHeld == true) wifiLock?.release() }
        wakeLock = null
        wifiLock = null
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
