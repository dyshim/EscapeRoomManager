package com.example.escaperoomtimer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.escaperoomtimer.MainActivity
import com.example.escaperoomtimer.R
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.util.formatTime

class RoomStatusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        TimerManager.initialize(context.applicationContext)
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context))
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        TimerManager.initialize(context.applicationContext)
        updateAll(context)
    }

    companion object {
        private val roomNameViewIds = intArrayOf(
            R.id.widget_room_1_name,
            R.id.widget_room_2_name,
            R.id.widget_room_3_name,
            R.id.widget_room_4_name,
            R.id.widget_room_5_name
        )

        private val roomTimeViewIds = intArrayOf(
            R.id.widget_room_1_time,
            R.id.widget_room_2_time,
            R.id.widget_room_3_time,
            R.id.widget_room_4_time,
            R.id.widget_room_5_time
        )

        fun updateAll(context: Context) {
            val appContext = context.applicationContext
            TimerManager.initialize(appContext)

            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val componentName = ComponentName(appContext, RoomStatusWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(appWidgetId, buildViews(appContext))
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_room_status)
            val rooms = TimerManager.rooms.take(roomNameViewIds.size)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                2001,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            roomNameViewIds.indices.forEach { index ->
                val room = rooms.getOrNull(index)
                if (room == null) {
                    views.setTextViewText(roomNameViewIds[index], "-")
                    views.setTextViewText(roomTimeViewIds[index], "")
                    views.setTextColor(roomTimeViewIds[index], 0xFF777777.toInt())
                } else {
                    views.setTextViewText(roomNameViewIds[index], room.name)
                    views.setTextViewText(roomTimeViewIds[index], statusText(room))
                    views.setTextColor(roomTimeViewIds[index], statusColor(room))
                }
            }

            return views
        }

        private fun statusText(room: RoomInfo): String = when (room.status) {
            RoomStatus.WAITING -> "대기중"
            RoomStatus.FINISHED -> "종료"
            else -> formatTime(room.seconds)
        }

        private fun statusColor(room: RoomInfo): Int = when (room.status) {
            RoomStatus.RUNNING -> 0xFF4CD964.toInt()
            RoomStatus.WARNING -> 0xFFFF4B4B.toInt()
            RoomStatus.PAUSED -> 0xFFFFB000.toInt()
            RoomStatus.WAITING -> 0xFFB0B0B0.toInt()
            RoomStatus.FINISHED -> 0xFF777777.toInt()
        }
    }
}
