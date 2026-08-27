package com.example.escaperoomtimer.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun TimeWheelInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    maxValue: Int,
    maxDigits: Int,
    accentColor: Color = Color(0xFF7134C8),
    enabled: Boolean = true
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val activeAccentColor = if (enabled) accentColor else accentColor.copy(alpha = 0.38f)
    val primaryTextColor = if (enabled) Color.White else Color.White.copy(alpha = 0.38f)
    val secondaryTextColor = Color(0xFF687078).copy(alpha = if (enabled) 1f else 0.38f)

    fun changeBy(delta: Int) {
        val current = value.toIntOrNull() ?: 0
        onValueChange((current + delta).coerceIn(0, maxValue).toString())
    }

    Column(
        modifier = modifier.then(
            if (enabled) {
                Modifier.pointerInput(value, maxValue) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount
                            if (abs(dragDistance) >= 24f) {
                                changeBy(if (dragDistance < 0f) 1 else -1)
                                dragDistance = 0f
                            }
                        },
                        onDragEnd = { dragDistance = 0f },
                        onDragCancel = { dragDistance = 0f }
                    )
                }
            } else {
                Modifier
            }
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFFD7DEE4).copy(alpha = if (enabled) 1f else 0.38f), fontSize = 12.sp)
        Text(
            "⌃",
            color = activeAccentColor,
            fontSize = 22.sp,
            modifier = Modifier.clickable(enabled = enabled) { changeBy(1) }.padding(horizontal = 30.dp, vertical = 3.dp)
        )
        Text(
            text = ((value.toIntOrNull() ?: 0) - 1).coerceAtLeast(0).toString(),
            color = secondaryTextColor,
            fontSize = 17.sp
        )
        BasicTextField(
            value = value,
            enabled = enabled,
            onValueChange = { input ->
                val digits = input.filter(Char::isDigit).takeLast(maxDigits)
                if (digits.isEmpty()) onValueChange("")
                else onValueChange(digits.toInt().coerceAtMost(maxValue).toString())
            },
            singleLine = true,
            textStyle = TextStyle(
                color = primaryTextColor,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            decorationBox = { innerTextField ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    innerTextField()
                    HorizontalDivider(color = activeAccentColor, modifier = Modifier.padding(top = 3.dp))
                }
            }
        )
        Text(
            text = ((value.toIntOrNull() ?: 0) + 1).coerceAtMost(maxValue).toString(),
            color = secondaryTextColor,
            fontSize = 17.sp
        )
        Text(
            "⌄",
            color = activeAccentColor,
            fontSize = 22.sp,
            modifier = Modifier.clickable(enabled = enabled) { changeBy(-1) }.padding(horizontal = 30.dp, vertical = 3.dp)
        )
    }
}
