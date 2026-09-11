package com.example.escaperoomtimer.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class TimeField(val label: String, val unitSeconds: Long) {
    HOUR("시", 3_600L),
    MINUTE("분", 60L),
    SECOND("초", 1L)
}

@Composable
fun TimerResetConfirmationDialog(
    themeName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("타이머 초기화") },
        text = { Text("'$themeName' 타이머를 초기화할까요?") },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        confirmButton = { TextButton(onClick = onConfirm) { Text("초기화") } }
    )
}

@Composable
fun DirectTimeSetDialog(
    themeName: String,
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("시간 직접 설정 · $themeName") },
        text = {
            DirectTimeSetContent(
                initialSeconds = initialSeconds,
                enabled = true,
                onCancel = onDismiss,
                onApply = onApply
            )
        },
        confirmButton = {}
    )
}

@Composable
fun DirectTimeSetContent(
    initialSeconds: Int,
    enabled: Boolean,
    onCancel: () -> Unit,
    onApply: (Int) -> Unit
) {
    val safeInitial = initialSeconds.coerceAtLeast(0)
    var hours by remember(safeInitial) { mutableLongStateOf((safeInitial / 3_600).toLong()) }
    var minutes by remember(safeInitial) { mutableLongStateOf(((safeInitial % 3_600) / 60).toLong()) }
    var seconds by remember(safeInitial) { mutableLongStateOf((safeInitial % 60).toLong()) }
    var selected by remember { mutableStateOf(TimeField.MINUTE) }
    var overwriteNext by remember { mutableStateOf(true) }

    fun selectedValue(): Long = when (selected) {
        TimeField.HOUR -> hours
        TimeField.MINUTE -> minutes
        TimeField.SECOND -> seconds
    }

    fun setSelected(value: Long) {
        val safe = value.coerceIn(0L, Int.MAX_VALUE.toLong())
        when (selected) {
            TimeField.HOUR -> hours = safe
            TimeField.MINUTE -> minutes = safe
            TimeField.SECOND -> seconds = safe
        }
    }

    fun totalSeconds(): Long = (hours * 3_600L + minutes * 60L + seconds)
        .coerceIn(0L, Int.MAX_VALUE.toLong())

    fun setFromTotal(total: Long) {
        val safe = total.coerceIn(0L, Int.MAX_VALUE.toLong())
        hours = safe / 3_600L
        minutes = (safe % 3_600L) / 60L
        seconds = safe % 60L
    }

    fun adjustSelected(delta: Int) {
        setFromTotal(totalSeconds() + selected.unitSeconds * delta)
        overwriteNext = true
    }

    fun enterDigit(digit: Int) {
        val next = if (overwriteNext) digit.toLong() else selectedValue() * 10L + digit
        setSelected(next)
        overwriteNext = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeField.entries.forEachIndexed { index, field ->
                TimeFieldDisplay(
                    value = when (field) {
                        TimeField.HOUR -> hours
                        TimeField.MINUTE -> minutes
                        TimeField.SECOND -> seconds
                    },
                    label = field.label,
                    selected = selected == field,
                    enabled = enabled,
                    onClick = {
                        selected = field
                        overwriteNext = true
                    },
                    modifier = Modifier.weight(1f)
                )
                if (index < TimeField.entries.lastIndex) {
                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeypadButton("−1", enabled, { adjustSelected(-1) }, Modifier.weight(1f))
            KeypadButton("+1", enabled, { adjustSelected(1) }, Modifier.weight(1f))
        }
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit ->
                    KeypadButton(digit, enabled, { enterDigit(digit.toInt()) }, Modifier.weight(1f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeypadButton("전체삭제", enabled, {
                setSelected(0)
                overwriteNext = true
            }, Modifier.weight(1f))
            KeypadButton("0", enabled, { enterDigit(0) }, Modifier.weight(1f))
            KeypadButton("⌫", enabled, {
                setSelected(selectedValue() / 10L)
                overwriteNext = false
            }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("취소") }
            Button(
                onClick = { onApply(totalSeconds().toInt()) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7134C8))
            ) { Text("설정") }
        }
    }
}

@Composable
private fun TimeFieldDisplay(
    value: Long,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xFF9C6ADE)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(if (selected) accent.copy(alpha = 0.18f) else Color(0xFF11171B), RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.38f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        Text(label, color = Color(0xFF9EA7AD), fontSize = 11.sp)
    }
}

@Composable
private fun KeypadButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(42.dp),
        border = BorderStroke(1.dp, Color(0xFF4A5157))
    ) { Text(text, fontSize = if (text == "전체삭제") 12.sp else 16.sp) }
}
