package com.example.escaperoomdisplay.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.escaperoomdisplay.MainActivity
import com.example.escaperoomdisplay.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DisplayRoomWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context))
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, DisplayRoomWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { widgetId ->
                manager.updateAppWidget(widgetId, buildViews(appContext))
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_display_room)
            val state = DisplayWidgetStateRepository.load(context)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                3101,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.display_widget_root, pendingIntent)

            if (state == null) {
                views.setTextViewText(R.id.display_widget_room_name, "방을 선택해 주세요")
                views.setTextViewText(R.id.display_widget_time, "--:--")
                views.setTextViewText(R.id.display_widget_end, "시작 후 표시")
                views.setTextViewText(R.id.display_widget_connection, "앱을 열어 연결해 주세요")
                views.setTextColor(R.id.display_widget_time, 0xFF9AA4AD.toInt())
                return views
            }

            views.setTextViewText(R.id.display_widget_room_name, state.roomName)
            views.setTextViewText(R.id.display_widget_time, formatTime(state.seconds))
            views.setTextViewText(R.id.display_widget_end, expectedEndText(state))
            views.setTextViewText(
                R.id.display_widget_connection,
                if (state.connected) "직원용 앱 연결됨" else "연결 끊김"
            )
            views.setTextColor(R.id.display_widget_time, timeColor(state.seconds, state.connected))
            views.setTextColor(
                R.id.display_widget_connection,
                if (state.connected) 0xFF44D17A.toInt() else 0xFFFF4B4B.toInt()
            )
            return views
        }

        private fun expectedEndText(state: DisplayWidgetStateRepository.SavedState): String {
            return when {
                state.status == "FINISHED" || state.seconds <= 0 -> "종료됨"
                state.isRunning -> {
                    val expectedAt = System.currentTimeMillis() + state.seconds * 1_000L
                    "종료 예정 ${SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(expectedAt))}"
                }
                state.status == "PAUSED" -> "일시정지 중"
                else -> "시작 후 표시"
            }
        }

        private fun formatTime(totalSeconds: Int): String {
            val safe = totalSeconds.coerceAtLeast(0)
            return "%02d:%02d".format(safe / 60, safe % 60)
        }

        private fun timeColor(seconds: Int, connected: Boolean): Int = when {
            !connected -> 0xFF9AA4AD.toInt()
            seconds <= 5 * 60 -> 0xFFFF4B4B.toInt()
            seconds <= 10 * 60 -> 0xFFFFA726.toInt()
            else -> 0xFFFFFFFF.toInt()
        }
    }
}
