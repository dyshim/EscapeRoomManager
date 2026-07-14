package com.example.escaperoomtimer.service

import android.app.Service
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.PowerManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import com.example.escaperoomtimer.alarm.ManagerGameEndAlarmController
import com.example.escaperoomtimer.manager.HintProgressManager
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.notification.TimerNotificationHelper
import com.example.escaperoomtimer.network.ManagerTcpServer
import com.example.escaperoomtimer.widget.RoomStatusWidgetProvider
import com.example.escaperoomtimer.web.ManagerWebServer

class TimerForegroundService : Service() {
    private companion object {
        const val TAG = "TimerForegroundService"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var heartbeatExecutor: ScheduledExecutorService? = null

    private val ticker = object : Runnable {
        override fun run() {
            TimerManager.tickAll()
            if (TimerManager.consumeNaturallyCompletedRoomIds().isNotEmpty()) {
                ManagerGameEndAlarmController.play(applicationContext)
            }
            updateNotification()
            handler.postDelayed(this, 1000L)
        }
    }

    private val webServerWatchdog = object : Runnable {
        override fun run() {
            runCatching { ManagerWebServer.ensureStarted(applicationContext) }
                .onFailure { error -> Log.e(TAG, "web server watchdog failed", error) }
            handler.postDelayed(this, 5_000L)
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
        handler.post(webServerWatchdog)
        startHeartbeat()
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
        handler.removeCallbacks(webServerWatchdog)
        heartbeatExecutor?.shutdownNow()
        heartbeatExecutor = null
        releaseRuntimeLocks()
        super.onDestroy()
    }


    @Synchronized
    private fun startHeartbeat() {
        if (heartbeatExecutor != null) return
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "manager-heartbeat").apply { isDaemon = true }
        }.also { executor ->
            executor.scheduleWithFixedDelay(
                {
                    runCatching { ManagerTcpServer.heartbeat() }
                        .onFailure { error -> Log.e(TAG, "TCP heartbeat failed", error) }
                },
                0L,
                5L,
                TimeUnit.SECONDS
            )
        }
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
        ManagerWebServer.broadcastState()
    }
}
