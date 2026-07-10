package com.example.escaperoomtimer.widget

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

class ManagerWidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val initial = ManagerWidgetStyleRepository.load(this, widgetId)
        setContent {
            var palette by remember { mutableStateOf(initial.palette) }
            var opacity by remember { mutableIntStateOf(initial.opacity) }
            MaterialTheme {
                WidgetConfigScreen(
                    title = "직원용 위젯 설정",
                    palettes = ManagerWidgetStyleRepository.Palette.entries.map { it.name to it.label },
                    selectedPalette = palette.name,
                    onPalette = { palette = ManagerWidgetStyleRepository.Palette.valueOf(it) },
                    opacity = opacity,
                    onOpacity = { opacity = it },
                    onSave = {
                        ManagerWidgetStyleRepository.save(this, widgetId, ManagerWidgetStyleRepository.Style(palette, opacity))
                        RoomStatusWidgetProvider.updateWidget(this, widgetId)
                        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    title: String,
    palettes: List<Pair<String, String>>,
    selectedPalette: String,
    onPalette: (String) -> Unit,
    opacity: Int,
    onOpacity: (Int) -> Unit,
    onSave: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF090B0D)).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("배경 색상", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        palettes.forEach { (key, label) ->
            Row(
                Modifier.fillMaxWidth().clickable { onPalette(key) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedPalette == key, onClick = { onPalette(key) })
                Text(label, color = Color.White, fontSize = 18.sp)
            }
        }
        Text("투명도", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(100, 85, 70).forEach { value ->
                FilterChip(selected = opacity == value, onClick = { onOpacity(value) }, label = { Text("$value%") })
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("적용", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
    }
}
