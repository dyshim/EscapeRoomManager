package com.example.escaperoomtimer.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.escaperoomtimer.MainActivity
import com.example.escaperoomtimer.R
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.util.formatTime

object TimerNotificationHelper {
    const val CHANNEL_ID = "timer_status_channel"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "타이머 진행 상태",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "진행 중인 테마 타이머를 상단바에 표시합니다."
                setShowBadge(false)
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun buildTimerNotification(context: Context): Notification {
        createChannel(context)

        val room = selectNotificationRoom()
        val title = room?.name ?: "운영 대시보드"
        val text = room?.let { roomInfo ->
            when (roomInfo.status) {
                RoomStatus.WAITING -> "대기"
                RoomStatus.RUNNING -> "${formatTime(roomInfo.seconds)} 남음 · 진행 중"
                RoomStatus.WARNING -> "${formatTime(roomInfo.seconds)} 남음 · 5분 이하"
                RoomStatus.PAUSED -> "${formatTime(roomInfo.seconds)} 남음 · 일시정지"
                RoomStatus.FINISHED -> "종료"
            }
        } ?: "진행 중인 타이머가 없습니다."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun selectNotificationRoom(): RoomInfo? {
        return TimerManager.enabledRooms.firstOrNull { it.isRunning }
            ?: TimerManager.enabledRooms.firstOrNull { it.status == RoomStatus.WARNING }
            ?: TimerManager.enabledRooms.firstOrNull { it.status == RoomStatus.PAUSED }
            ?: TimerManager.enabledRooms.firstOrNull()
    }
}
