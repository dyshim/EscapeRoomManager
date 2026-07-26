package com.example.escaperoomtimer.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val TimeAccent = Color(0xFF20D86F)

@Composable
fun DefaultTimePicker(
    totalSeconds: Int,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeTotal = totalSeconds.coerceIn(1, 240 * 60)
    val minutes = safeTotal / 60
    val seconds = safeTotal % 60

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeWheelInput(
                label = "분",
                value = minutes.toString(),
                maxValue = 240,
                maxDigits = 3,
                modifier = Modifier.weight(1f),
                accentColor = TimeAccent,
                onValueChange = { next ->
                    onSecondsChange(((next.toIntOrNull() ?: 0) * 60 + seconds).coerceIn(1, 240 * 60))
                }
            )
            Text(":", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            TimeWheelInput(
                label = "초",
                value = seconds.toString(),
                maxValue = 59,
                maxDigits = 2,
                modifier = Modifier.weight(1f),
                accentColor = TimeAccent,
                onValueChange = { next ->
                    onSecondsChange((minutes * 60 + (next.toIntOrNull() ?: 0)).coerceIn(1, 240 * 60))
                }
            )
        }
        Text(
            "위아래로 밀거나 숫자를 눌러 입력하세요.",
            color = Color(0xFF8D96A0),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(45, 60, 90).forEach { preset ->
                val selected = safeTotal == preset * 60
                OutlinedButton(
                    onClick = { onSecondsChange(preset * 60) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (selected) TimeAccent else Color(0xFF46515A))
                ) {
                    Text("${preset}분", color = if (selected) TimeAccent else Color.White)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("설정 시간", color = Color(0xFFB8C0C8), fontSize = 13.sp)
            Text(formatDefaultTime(safeTotal), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun formatDefaultTime(totalSeconds: Int): String =
    "%02d:%02d".format(totalSeconds.coerceAtLeast(0) / 60, totalSeconds.coerceAtLeast(0) % 60)
