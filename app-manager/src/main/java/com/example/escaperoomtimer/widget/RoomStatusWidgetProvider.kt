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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoomStatusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        TimerManager.initialize(context.applicationContext)
        appWidgetIds.forEach { updateWidget(context, it) }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        TimerManager.initialize(context.applicationContext)
        updateAll(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { ManagerWidgetStyleRepository.delete(context, it) }
        super.onDeleted(context, appWidgetIds)
    }

    companion object {
        private val roomNameViewIds = intArrayOf(
            R.id.widget_room_1_name, R.id.widget_room_2_name, R.id.widget_room_3_name,
            R.id.widget_room_4_name, R.id.widget_room_5_name
        )
        private val roomTimeViewIds = intArrayOf(
            R.id.widget_room_1_time, R.id.widget_room_2_time, R.id.widget_room_3_time,
            R.id.widget_room_4_time, R.id.widget_room_5_time
        )
        private val roomEndViewIds = intArrayOf(
            R.id.widget_room_1_end, R.id.widget_room_2_end, R.id.widget_room_3_end,
            R.id.widget_room_4_end, R.id.widget_room_5_end
        )

        fun updateAll(context: Context) {
            val appContext = context.applicationContext
            TimerManager.initialize(appContext)
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, RoomStatusWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { updateWidget(appContext, it) }
        }

        fun updateWidget(context: Context, widgetId: Int) {
            TimerManager.initialize(context.applicationContext)
            AppWidgetManager.getInstance(context).updateAppWidget(widgetId, buildViews(context, widgetId))
        }

        private fun buildViews(context: Context, widgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_room_status)
            views.setInt(R.id.widget_root, "setBackgroundResource", ManagerWidgetStyleRepository.backgroundRes(ManagerWidgetStyleRepository.load(context, widgetId)))
            val rooms = TimerManager.enabledRooms.take(roomNameViewIds.size)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_content,
                PendingIntent.getActivity(context, 2001 + widgetId, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val configIntent = Intent(context, ManagerWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            views.setOnClickPendingIntent(
                R.id.widget_settings,
                PendingIntent.getActivity(context, 5000 + widgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            roomNameViewIds.indices.forEach { index ->
                val room = rooms.getOrNull(index)
                if (room == null) {
                    views.setTextViewText(roomNameViewIds[index], "-")
                    views.setTextViewText(roomTimeViewIds[index], "")
                    views.setTextViewText(roomEndViewIds[index], "")
                    views.setTextColor(roomTimeViewIds[index], 0xFF777777.toInt())
                } else {
                    views.setTextViewText(roomNameViewIds[index], room.name)
                    views.setTextViewText(roomTimeViewIds[index], statusText(room))
                    views.setTextViewText(roomEndViewIds[index], expectedEndText(room))
                    views.setTextColor(roomTimeViewIds[index], statusColor(room))
                }
            }
            return views
        }

        private fun statusText(room: RoomInfo): String = if (room.isMaintenance) "유지보수" else when (room.status) {
            RoomStatus.WAITING -> "대기중"
            RoomStatus.FINISHED -> "종료"
            else -> formatTime(room.seconds)
        }

        private fun expectedEndText(room: RoomInfo): String = when {
            room.isMaintenance -> "손님용 숨김"
            room.status == RoomStatus.FINISHED || room.seconds <= 0 -> "종료됨"
            room.isRunning -> "종료 ${formatExpectedTime(System.currentTimeMillis() + room.seconds * 1_000L)}"
            room.status == RoomStatus.PAUSED -> "일시정지"
            else -> "시작 후 표시"
        }

        private fun formatExpectedTime(timeMillis: Long) = SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(timeMillis))

        private fun statusColor(room: RoomInfo): Int = if (room.isMaintenance) 0xFF64B5F6.toInt() else when (room.status) {
            RoomStatus.RUNNING -> 0xFF4CD964.toInt()
            RoomStatus.WARNING -> 0xFFFF4B4B.toInt()
            RoomStatus.PAUSED -> 0xFFFFB000.toInt()
            RoomStatus.WAITING -> 0xFFE0E0E0.toInt()
            RoomStatus.FINISHED -> 0xFF9E9E9E.toInt()
        }
    }
}
