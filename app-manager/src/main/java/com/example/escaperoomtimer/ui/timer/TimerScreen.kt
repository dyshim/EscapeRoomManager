package com.example.escaperoomtimer.ui.timer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.alarm.ManagerGameEndAlarmController
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.ui.common.ManagerStatusColors
import com.example.escaperoomtimer.util.formatTime
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val TIMER_UI_PREFS = "manager_timer_ui"
private const val KEY_TIME_ADJUSTMENT_EXPANDED = "time_adjustment_expanded"

@Composable
fun TimerScreen(
    roomId: String,
    onBack: () -> Unit
) {
    val room = TimerManager.getRoom(roomId) ?: return
    val alarmActive by ManagerGameEndAlarmController.isActive
    val wifiConnected = rememberWifiConnected()
    val context = LocalContext.current
    val uiPreferences = remember(context) {
        context.getSharedPreferences(TIMER_UI_PREFS, Context.MODE_PRIVATE)
    }

    var minuteInput by remember(room.id) { mutableStateOf((room.seconds / 60).toString()) }
    var secondInput by remember(room.id) { mutableStateOf((room.seconds % 60).toString()) }
    var undoSnapshot by remember(room.id) { mutableStateOf<UndoSnapshot?>(null) }
    var undoVersion by remember(room.id) { mutableIntStateOf(0) }
    var adjustmentIndex by remember(room.id) { mutableIntStateOf(2) }
    var directInputExpanded by remember(room.id) { mutableStateOf(false) }
    var resetConfirmationVisible by remember(room.id) { mutableStateOf(false) }
    var timeAdjustmentExpanded by remember(uiPreferences) {
        mutableStateOf(uiPreferences.getBoolean(KEY_TIME_ADJUSTMENT_EXPANDED, true))
    }
    val adjustmentOptions = remember { listOf(30, 60, 5 * 60, 10 * 60) }

    LaunchedEffect(undoVersion) {
        if (undoVersion > 0) {
            delay(5_000L)
            undoSnapshot = null
        }
    }

    fun rememberBeforeChange() {
        undoSnapshot = UndoSnapshot(
            seconds = room.seconds,
            isRunning = room.isRunning,
            status = room.status,
            startedAtEpochMillis = room.startedAtEpochMillis,
            finishedAtEpochMillis = room.finishedAtEpochMillis
        )
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "←",
                color = Color.White,
                fontSize = 32.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable { onBack() }
            )
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = room.name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
                StatusBadge(room.status, room.isRunning, room.isMaintenance)
            }
            WifiStatusIcon(
                connected = wifiConnected,
                color = if (wifiConnected) ManagerStatusColors.Connected else ManagerStatusColors.Disconnected,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF11171B), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (room.status == RoomStatus.WAITING) "기본 시간" else "남은 시간",
                color = Color(0xFFD7DEE4),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (room.isMaintenance) "—" else formatTime(room.seconds),
                color = timerColor(room.seconds, room.status, room.isMaintenance),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            if (!room.isMaintenance) {
                Text(text = "분 : 초", color = Color(0xFF8F989F), fontSize = 12.sp)
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                color = Color(0xFF343D44)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerClockInfo(
                    label = "시작 시간",
                    value = recordedTimeText(room.startedAtEpochMillis, "-"),
                    modifier = Modifier.weight(1f)
                )
                Box(modifier = Modifier.size(width = 1.dp, height = 40.dp).background(Color(0xFF343D44)))
                TimerClockInfo(
                    label = if (room.status == RoomStatus.FINISHED || room.seconds <= 0) "실제 종료" else "종료 예정",
                    value = expectedEndTimeText(
                        seconds = room.seconds,
                        status = room.status,
                        isRunning = room.isRunning,
                        finishedAtEpochMillis = room.finishedAtEpochMillis
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                color = Color(0xFF343D44)
            )
            Text(
                text = "소요 시간  ${formatTime(room.elapsedSeconds)}",
                color = Color(0xFFB7BEC4),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (room.status == RoomStatus.FINISHED && alarmActive) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = ManagerGameEndAlarmController::stop,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ManagerStatusColors.Finished)
            ) {
                Text("⊘  알람 끄기", color = ManagerStatusColors.Finished, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (room.isMaintenance) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF174A6E), RoundedCornerShape(14.dp))
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "유지보수 중 · 타이머 시작이 잠겨 있습니다",
                    color = Color(0xFF9ED8FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            if (room.status != RoomStatus.FINISHED && room.seconds > 0) {
                TimerButton(
                    text = when {
                        room.isRunning -> "Ⅱ 일시정지"
                        room.status == RoomStatus.PAUSED -> "▶ 계속"
                        else -> "▶ 시작"
                    },
                    color = when {
                        room.isRunning -> Color(0xFFFFC107)
                        room.status == RoomStatus.PAUSED -> Color(0xFF16C967)
                        else -> Color(0xFF7134C8)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { TimerManager.startOrPause(room.id) }
                )
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = { resetConfirmationVisible = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D858B))
            ) {
                Text("↺  초기화", color = Color.White, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF11171B), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        timeAdjustmentExpanded = !timeAdjustmentExpanded
                        uiPreferences.edit()
                            .putBoolean(KEY_TIME_ADJUSTMENT_EXPANDED, timeAdjustmentExpanded)
                            .apply()
                    }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("시간 조정", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(if (timeAdjustmentExpanded) "⌃" else "⌄", color = Color.White, fontSize = 18.sp)
            }

            if (timeAdjustmentExpanded) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { adjustmentIndex = (adjustmentIndex - 1).coerceAtLeast(0) },
                        modifier = Modifier.weight(1f).height(54.dp),
                        enabled = adjustmentIndex > 0
                    ) { Text("−", fontSize = 26.sp) }
                    Text(
                        text = adjustmentLabel(adjustmentOptions[adjustmentIndex]),
                        modifier = Modifier.weight(1.2f),
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = { adjustmentIndex = (adjustmentIndex + 1).coerceAtMost(adjustmentOptions.lastIndex) },
                        modifier = Modifier.weight(1f).height(54.dp),
                        enabled = adjustmentIndex < adjustmentOptions.lastIndex
                    ) { Text("+", fontSize = 26.sp) }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimerButton(
                        text = "시간 추가",
                        color = Color(0xFF16C967),
                        modifier = Modifier.weight(1f),
                        onClick = { adjustTime(adjustmentOptions[adjustmentIndex]) }
                    )
                    TimerButton(
                        text = "시간 차감",
                        color = Color(0xFFFF414D),
                        modifier = Modifier.weight(1f),
                        onClick = { adjustTime(-adjustmentOptions[adjustmentIndex]) }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    adjustmentOptions.forEachIndexed { index, seconds ->
                        OutlinedButton(
                            onClick = { adjustmentIndex = index },
                            modifier = Modifier.weight(1f),
                            colors = if (adjustmentIndex == index) {
                                ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF164D25))
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                        ) {
                            Text(adjustmentLabel(seconds), fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF11171B), RoundedCornerShape(12.dp))
                .clickable { directInputExpanded = !directInputExpanded }
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("남은 시간 직접 설정", color = Color.White, fontWeight = FontWeight.Bold)
            Text(if (directInputExpanded) "⌃" else "⌄", color = Color.White)
        }

        if (directInputExpanded) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF11171B), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DirectTimeInput(
                        value = minuteInput,
                        onValueChange = { minuteInput = it },
                        modifier = Modifier.weight(1f),
                        label = "분",
                        maxValue = 999,
                        maxDigits = 3
                    )
                    Text(":", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    DirectTimeInput(
                        value = secondInput,
                        onValueChange = { secondInput = it },
                        modifier = Modifier.weight(1f),
                        label = "초",
                        maxValue = 59,
                        maxDigits = 2
                    )
                }
                Text(
                    "위아래로 밀거나 숫자를 눌러 입력하세요.",
                    color = Color(0xFF9EA7AD),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp, bottom = 8.dp)
                )
                TimerButton(
                    text = "입력 시간 적용",
                    color = Color(0xFF7134C8),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val minutes = minuteInput.toIntOrNull()?.coerceIn(0, 999) ?: 0
                        val seconds = secondInput.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        rememberBeforeChange()
                        TimerManager.setTime(room.id, minutes * 60 + seconds)
                    }
                )
            }
        }

        if (resetConfirmationVisible) {
            AlertDialog(
                onDismissRequest = { resetConfirmationVisible = false },
                title = { Text("초기화") },
                text = {
                    Text(
                        "남은 시간을 ${formatTime(room.defaultMinutes * 60)}으로 되돌릴까요?\n" +
                            "시작 및 종료 시간 기록도 초기화됩니다."
                    )
                },
                dismissButton = {
                    TextButton(onClick = { resetConfirmationVisible = false }) { Text("취소") }
                },
                confirmButton = {
                    TextButton(onClick = {
                        TimerManager.reset(room.id)
                        minuteInput = room.defaultMinutes.toString()
                        secondInput = "0"
                        resetConfirmationVisible = false
                    }) { Text("초기화", color = Color(0xFF9C6ADE)) }
                }
            )
        }

        undoSnapshot?.let { snapshot ->
            Spacer(Modifier.height(12.dp))
            UndoBar(
                previousSeconds = snapshot.seconds,
                onUndo = {
                    TimerManager.restoreTimeState(
                        roomId = room.id,
                        seconds = snapshot.seconds,
                        isRunning = snapshot.isRunning,
                        status = snapshot.status,
                        startedAtEpochMillis = snapshot.startedAtEpochMillis,
                        finishedAtEpochMillis = snapshot.finishedAtEpochMillis
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
    val status: RoomStatus,
    val startedAtEpochMillis: Long?,
    val finishedAtEpochMillis: Long?
)

@Composable
private fun DirectTimeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    maxValue: Int,
    maxDigits: Int
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }

    fun changeBy(delta: Int) {
        val current = value.toIntOrNull() ?: 0
        onValueChange((current + delta).coerceIn(0, maxValue).toString())
    }

    Column(
        modifier = modifier.pointerInput(value, maxValue) {
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
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFFD7DEE4), fontSize = 12.sp)
        Text(
            "⌃",
            color = Color(0xFF7134C8),
            fontSize = 22.sp,
            modifier = Modifier.clickable { changeBy(1) }.padding(horizontal = 30.dp, vertical = 3.dp)
        )
        Text(
            text = ((value.toIntOrNull() ?: 0) - 1).coerceAtLeast(0).toString(),
            color = Color(0xFF687078),
            fontSize = 17.sp
        )
        BasicTextField(
            value = value,
            onValueChange = { input ->
                val digits = input.filter(Char::isDigit).take(maxDigits)
                if (digits.isEmpty()) {
                    onValueChange("")
                } else {
                    onValueChange(digits.toInt().coerceAtMost(maxValue).toString())
                }
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            decorationBox = { innerTextField ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    innerTextField()
                    HorizontalDivider(color = Color(0xFF7134C8), modifier = Modifier.padding(top = 3.dp))
                }
            }
        )
        Text(
            text = ((value.toIntOrNull() ?: 0) + 1).coerceAtMost(maxValue).toString(),
            color = Color(0xFF687078),
            fontSize = 17.sp
        )
        Text(
            "⌄",
            color = Color(0xFF7134C8),
            fontSize = 22.sp,
            modifier = Modifier.clickable { changeBy(-1) }.padding(horizontal = 30.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun rememberWifiConnected(): Boolean {
    val context = LocalContext.current
    val connectivityManager = remember(context) {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    var connected by remember(connectivityManager) {
        mutableStateOf(connectivityManager.hasWifiConnection())
    }

    DisposableEffect(connectivityManager) {
        val mainHandler = Handler(Looper.getMainLooper())
        val refresh = {
            mainHandler.post { connected = connectivityManager.hasWifiConnection() }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                refresh()
            }

            override fun onLost(network: Network) {
                refresh()
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                refresh()
            }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        runCatching { connectivityManager.registerNetworkCallback(request, callback) }

        onDispose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            mainHandler.removeCallbacksAndMessages(null)
        }
    }

    return connected
}

private fun ConnectivityManager.hasWifiConnection(): Boolean =
    allNetworks.any { network ->
        getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

@Composable
private fun WifiConnectionStatus(
    connected: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (connected) ManagerStatusColors.Connected else ManagerStatusColors.Disconnected
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WifiStatusIcon(connected = connected, color = color)
        Text(
            text = if (connected) "연결됨" else "연결 끊김",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WifiStatusIcon(
    connected: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 15.dp, height = 13.dp)) {
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = color,
            startAngle = 215f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(size.width * 0.05f, 0f),
            size = Size(size.width * 0.90f, size.height * 0.90f),
            style = stroke
        )
        drawArc(
            color = color,
            startAngle = 215f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(size.width * 0.28f, size.height * 0.32f),
            size = Size(size.width * 0.44f, size.height * 0.44f),
            style = stroke
        )
        drawCircle(
            color = color,
            radius = size.minDimension * 0.07f,
            center = Offset(size.width / 2f, size.height * 0.88f)
        )
        if (!connected) {
            drawLine(
                color = color,
                start = Offset(size.width * 0.08f, size.height * 0.08f),
                end = Offset(size.width * 0.92f, size.height * 0.94f),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun TimerClockInfo(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color(0xFFD7DEE4),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
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
fun StatusBadge(status: RoomStatus, isRunning: Boolean, isMaintenance: Boolean = false) {
    val label = if (isMaintenance) "유지보수" else when (status) {
        RoomStatus.WAITING -> "대기"
        RoomStatus.RUNNING -> "진행 중"
        RoomStatus.WARNING -> if (isRunning) "진행 중" else "일시정지"
        RoomStatus.PAUSED -> "일시정지"
        RoomStatus.FINISHED -> "종료"
    }

    val color = if (isMaintenance) ManagerStatusColors.Maintenance else when (status) {
        RoomStatus.WAITING -> ManagerStatusColors.Waiting
        RoomStatus.RUNNING -> ManagerStatusColors.Running
        RoomStatus.WARNING -> if (isRunning) ManagerStatusColors.Running else ManagerStatusColors.Paused
        RoomStatus.PAUSED -> ManagerStatusColors.Paused
        RoomStatus.FINISHED -> ManagerStatusColors.Finished
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

private fun expectedEndTimeText(
    seconds: Int,
    status: RoomStatus,
    isRunning: Boolean,
    finishedAtEpochMillis: Long?
): String {
    return when {
        status == RoomStatus.FINISHED || seconds <= 0 ->
            recordedTimeText(finishedAtEpochMillis, "-")
        status == RoomStatus.WAITING -> "-"
        !isRunning -> "일시정지 중"
        else -> {
            val endAtMillis = System.currentTimeMillis() + seconds * 1_000L
            formatClockTime(endAtMillis)
        }
    }
}

private fun recordedTimeText(epochMillis: Long?, emptyText: String): String =
    epochMillis?.let(::formatClockTime) ?: emptyText

private fun formatClockTime(epochMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(epochMillis))

private fun adjustmentLabel(seconds: Int): String =
    if (seconds < 60) "${seconds}초" else "${seconds / 60}분"

fun timerColor(seconds: Int, status: RoomStatus, isMaintenance: Boolean = false): Color {
    return when {
        isMaintenance -> ManagerStatusColors.Maintenance
        status == RoomStatus.FINISHED || seconds <= 0 -> ManagerStatusColors.Finished
        seconds <= 5 * 60 -> Color(0xFFFF4B4B)
        seconds <= 10 * 60 -> Color(0xFFFFA726)
        else -> Color(0xFF42E66F)
    }
}
