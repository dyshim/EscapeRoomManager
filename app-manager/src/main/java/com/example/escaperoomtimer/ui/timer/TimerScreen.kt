package com.example.escaperoomtimer.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.alarm.ManagerGameEndAlarmController
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.util.formatTime
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimerScreen(
    roomId: String,
    onBack: () -> Unit
) {
    val room = TimerManager.getRoom(roomId) ?: return
    val alarmActive by ManagerGameEndAlarmController.isActive

    var minuteInput by remember(room.id) { mutableStateOf((room.seconds / 60).toString()) }
    var secondInput by remember(room.id) { mutableStateOf((room.seconds % 60).toString()) }
    var undoSnapshot by remember(room.id) { mutableStateOf<UndoSnapshot?>(null) }
    var undoVersion by remember(room.id) { mutableIntStateOf(0) }

    LaunchedEffect(undoVersion) {
        if (undoVersion > 0) {
            delay(5_000L)
            undoSnapshot = null
        }
    }

    fun rememberBeforeChange() {
        undoSnapshot = UndoSnapshot(room.seconds, room.isRunning, room.status)
        undoVersion += 1
    }

    fun adjustTime(deltaSeconds: Int) {
        rememberBeforeChange()
        TimerManager.adjustSeconds(room.id, deltaSeconds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = Color.White,
                fontSize = 32.sp,
                modifier = Modifier.clickable { onBack() }
            )
            Text(room.name, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("직원", color = Color(0xFFFFB000), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
        StatusBadge(room.status, room.isRunning)
        Spacer(Modifier.height(14.dp))

        Text(
            text = "남은 시간",
            color = Color(0xFFD7DEE4),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatTime(room.seconds),
            color = timerColor(room.seconds, room.status),
            fontSize = 78.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = statusLabel(room.status),
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        if (room.status == RoomStatus.FINISHED && alarmActive) {
            Spacer(Modifier.height(12.dp))
            TimerButton(
                text = "알람 끄기",
                color = Color(0xFF9B211B),
                modifier = Modifier.fillMaxWidth(),
                onClick = ManagerGameEndAlarmController::stop
            )
        }

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF171C20), RoundedCornerShape(14.dp))
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "종료 예정",
                    color = Color(0xFFD7DEE4),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = expectedEndTimeText(
                        seconds = room.seconds,
                        status = room.status,
                        isRunning = room.isRunning
                    ),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimerButton(
                text = if (room.isRunning) "Ⅱ 일시정지" else "▶ 시작",
                color = if (room.isRunning) Color(0xFFC96D00) else Color(0xFF0D6B24),
                modifier = Modifier.weight(1f),
                onClick = { TimerManager.startOrPause(room.id) }
            )
            TimerButton(
                text = "■ 종료",
                color = Color(0xFF9B211B),
                modifier = Modifier.weight(1f),
                onClick = {
                    rememberBeforeChange()
                    TimerManager.stop(room.id)
                }
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("시간 추가")
        Spacer(Modifier.height(8.dp))

        TimeAdjustRow(
            leftText = "+10분",
            rightText = "+5분",
            leftClick = { adjustTime(10 * 60) },
            rightClick = { adjustTime(5 * 60) }
        )
        Spacer(Modifier.height(8.dp))
        TimeAdjustRow(
            leftText = "+1분",
            rightText = "+30초",
            leftClick = { adjustTime(60) },
            rightClick = { adjustTime(30) }
        )
        Spacer(Modifier.height(8.dp))
        TimerButton(
            text = "+10초",
            color = Color(0xFF8B4A00),
            modifier = Modifier.fillMaxWidth(),
            onClick = { adjustTime(10) }
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("시간 차감")
        Spacer(Modifier.height(8.dp))

        TimeAdjustRow(
            leftText = "-10분",
            rightText = "-5분",
            leftClick = { adjustTime(-10 * 60) },
            rightClick = { adjustTime(-5 * 60) }
        )
        Spacer(Modifier.height(8.dp))
        TimeAdjustRow(
            leftText = "-1분",
            rightText = "-30초",
            leftClick = { adjustTime(-60) },
            rightClick = { adjustTime(-30) }
        )
        Spacer(Modifier.height(8.dp))
        TimerButton(
            text = "-10초",
            color = Color(0xFF242A2F),
            modifier = Modifier.fillMaxWidth(),
            onClick = { adjustTime(-10) }
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("직접 입력")
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = minuteInput,
                onValueChange = { value ->
                    minuteInput = value.filter(Char::isDigit).take(3)
                },
                modifier = Modifier.weight(1f),
                label = { Text("분") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = secondInput,
                onValueChange = { value ->
                    secondInput = value.filter(Char::isDigit).take(2)
                },
                modifier = Modifier.weight(1f),
                label = { Text("초") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(Modifier.height(10.dp))
        TimerButton(
            text = "입력한 시간 적용",
            color = Color(0xFF4E2D78),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val minutes = minuteInput.toIntOrNull()?.coerceIn(0, 999) ?: 0
                val seconds = secondInput.toIntOrNull()?.coerceIn(0, 59) ?: 0
                rememberBeforeChange()
                TimerManager.setTime(room.id, minutes * 60 + seconds)
                minuteInput = minutes.toString()
                secondInput = seconds.toString()
            }
        )

        Spacer(Modifier.height(10.dp))
        TimerButton(
            text = "↺ 기본시간 초기화",
            color = Color(0xFF242A2F),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                TimerManager.reset(room.id)
                minuteInput = room.defaultMinutes.toString()
                secondInput = "0"
            }
        )

        undoSnapshot?.let { snapshot ->
            Spacer(Modifier.height(12.dp))
            UndoBar(
                previousSeconds = snapshot.seconds,
                onUndo = {
                    TimerManager.restoreTimeState(
                        roomId = room.id,
                        seconds = snapshot.seconds,
                        isRunning = snapshot.isRunning,
                        status = snapshot.status
                    )
                    minuteInput = (snapshot.seconds / 60).toString()
                    secondInput = (snapshot.seconds % 60).toString()
                    undoSnapshot = null
                }
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

private data class UndoSnapshot(
    val seconds: Int,
    val isRunning: Boolean,
    val status: RoomStatus
)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFD6D6D6),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun TimeAdjustRow(
    leftText: String,
    rightText: String,
    leftClick: () -> Unit,
    rightClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TimerButton(
            text = leftText,
            color = Color(0xFF8B4A00),
            modifier = Modifier.weight(1f),
            onClick = leftClick
        )
        TimerButton(
            text = rightText,
            color = Color(0xFF8B4A00),
            modifier = Modifier.weight(1f),
            onClick = rightClick
        )
    }
}

@Composable
private fun UndoBar(
    previousSeconds: Int,
    onUndo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1D2530), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "이전 시간 ${formatTime(previousSeconds)}",
            color = Color.White,
            fontSize = 14.sp
        )
        TextButton(onClick = onUndo) {
            Text("실행 취소", color = Color(0xFFFFB000), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatusBadge(status: RoomStatus, isRunning: Boolean) {
    val label = when (status) {
        RoomStatus.WAITING -> "대기"
        RoomStatus.RUNNING -> "진행중"
        RoomStatus.WARNING -> if (isRunning) "5분 이하" else "일시정지"
        RoomStatus.PAUSED -> "일시정지"
        RoomStatus.FINISHED -> "종료"
    }

    val color = when (status) {
        RoomStatus.WAITING -> Color(0xFF555555)
        RoomStatus.RUNNING -> Color(0xFF0F4A1E)
        RoomStatus.WARNING -> Color(0xFF5A1C1C)
        RoomStatus.PAUSED -> Color(0xFF6A5300)
        RoomStatus.FINISHED -> Color(0xFF444444)
    }

    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(999.dp))
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TimerButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

private fun statusLabel(status: RoomStatus): String {
    return when (status) {
        RoomStatus.WAITING -> "대기중"
        RoomStatus.RUNNING -> "진행중"
        RoomStatus.WARNING -> "종료 임박"
        RoomStatus.PAUSED -> "일시정지"
        RoomStatus.FINISHED -> "종료"
    }
}

private fun expectedEndTimeText(
    seconds: Int,
    status: RoomStatus,
    isRunning: Boolean
): String {
    return when {
        status == RoomStatus.FINISHED || seconds <= 0 -> "종료됨"
        status == RoomStatus.WAITING -> "시작 후 표시"
        !isRunning -> "일시정지 중"
        else -> {
            val endAtMillis = System.currentTimeMillis() + seconds * 1_000L
            SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(endAtMillis))
        }
    }
}

fun timerColor(seconds: Int, status: RoomStatus): Color {
    return when {
        status == RoomStatus.FINISHED || seconds <= 0 -> Color(0xFFFF4B4B)
        seconds <= 5 * 60 -> Color(0xFFFF4B4B)
        seconds <= 10 * 60 -> Color(0xFFFFA726)
        else -> Color(0xFF42E66F)
    }
}
