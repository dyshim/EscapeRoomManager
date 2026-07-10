package com.example.escaperoomdisplay.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DisplayWidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val initial = DisplayWidgetStyleRepository.load(this, widgetId)
        setContent {
            var palette by remember { mutableStateOf(initial.palette) }
            var opacity by remember { mutableIntStateOf(initial.opacity) }
            MaterialTheme {
                Column(
                    Modifier.fillMaxSize().background(Color(0xFF090B0D)).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text("손님용 위젯 설정", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("배경 색상", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    DisplayWidgetStyleRepository.Palette.entries.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().clickable { palette = item }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = palette == item, onClick = { palette = item })
                            Text(item.label, color = Color.White, fontSize = 18.sp)
                        }
                    }
                    Text("투명도", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(100, 85, 70).forEach { value ->
                            FilterChip(selected = opacity == value, onClick = { opacity = value }, label = { Text("$value%") })
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            DisplayWidgetStyleRepository.save(this@DisplayWidgetConfigActivity, widgetId, DisplayWidgetStyleRepository.Style(palette, opacity))
                            DisplayRoomWidgetProvider.updateWidget(this@DisplayWidgetConfigActivity, widgetId)
                            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                            finish()
                        },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("적용", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
