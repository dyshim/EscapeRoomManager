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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            Text(
                text = room.name,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            WifiConnectionStatus(
                connected = wifiConnected,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        Spacer(Modifier.height(24.dp))
        StatusBadge(room.status, room.isRunning, room.isMaintenance)
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
            color = timerColor(room.seconds, room.status, room.isMaintenance),
            fontSize = 78.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "소요 시간 ${formatTime(room.elapsedSeconds)}",
            color = Color(0xFF9EA7AD),
            fontSize = 15.sp,
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
                .padding(horizontal = 12.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerClockInfo(
                    label = "시작 시간",
                    value = recordedTimeText(room.startedAtEpochMillis, "시작 후 표시"),
                    modifier = Modifier.weight(1f)
                )
                TimerClockInfo(
                    label = if (room.status == RoomStatus.FINISHED || room.seconds <= 0) {
                        "종료 시간"
                    } else {
                        "종료 예정"
                    },
                    value = expectedEndTimeText(
                        seconds = room.seconds,
                        status = room.status,
                        isRunning = room.isRunning,
                        finishedAtEpochMillis = room.finishedAtEpochMillis
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

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
        } else if (room.status != RoomStatus.FINISHED && room.seconds > 0) {
            TimerButton(
                text = when {
                    room.isRunning -> "Ⅱ 일시정지"
                    room.status == RoomStatus.PAUSED -> "▶ 계속"
                    else -> "▶ 시작"
                },
                color = if (room.isRunning) Color(0xFFC96D00) else Color(0xFF0D6B24),
                modifier = Modifier.fillMaxWidth(),
                onClick = { TimerManager.startOrPause(room.id) }
            )
        }

        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF171C20), RoundedCornerShape(14.dp))
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
                Text(if (timeAdjustmentExpanded) "⌃" else "›", color = Color.White, fontSize = 18.sp)
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
                        color = Color(0xFF0D6B24),
                        modifier = Modifier.weight(1f),
                        onClick = { adjustTime(adjustmentOptions[adjustmentIndex]) }
                    )
                    TimerButton(
                        text = "시간 차감",
                        color = Color(0xFFC96D00),
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
                .background(Color(0xFF171C20), RoundedCornerShape(12.dp))
                .clickable { directInputExpanded = !directInputExpanded }
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("남은 시간 직접 설정", color = Color.White, fontWeight = FontWeight.Bold)
            Text(if (directInputExpanded) "⌃" else "›", color = Color.White)
        }

        if (directInputExpanded) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = minuteInput,
                    onValueChange = { minuteInput = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.weight(1f),
                    label = { Text("분") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = secondInput,
                    onValueChange = { secondInput = it.filter(Char::isDigit).take(2) },
                    modifier = Modifier.weight(1f),
                    label = { Text("초") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(Modifier.height(8.dp))
            TimerButton(
                text = "입력 시간 적용",
                color = Color(0xFF4E2D78),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val minutes = minuteInput.toIntOrNull()?.coerceIn(0, 999) ?: 0
                    val seconds = secondInput.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    rememberBeforeChange()
                    TimerManager.setTime(room.id, minutes * 60 + seconds)
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { resetConfirmationVisible = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("↺ 기본 시간으로 초기화", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "설정된 기본 시간 ${formatTime(room.defaultMinutes * 60)}",
            color = Color(0xFF8C959B),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 5.dp)
        )

        if (resetConfirmationVisible) {
            AlertDialog(
                onDismissRequest = { resetConfirmationVisible = false },
                title = { Text("기본 시간으로 초기화") },
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
    val color = if (connected) Color(0xFF74C98C) else Color(0xFFE57373)
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
    color: Color
) {
    Canvas(modifier = Modifier.size(width = 15.dp, height = 13.dp)) {
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 20.sp,
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
        RoomStatus.RUNNING -> "진행중"
        RoomStatus.WARNING -> if (isRunning) "5분 이하" else "일시정지"
        RoomStatus.PAUSED -> "일시정지"
        RoomStatus.FINISHED -> "종료"
    }

    val color = if (isMaintenance) Color(0xFF174A6E) else when (status) {
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

private fun expectedEndTimeText(
    seconds: Int,
    status: RoomStatus,
    isRunning: Boolean,
    finishedAtEpochMillis: Long?
): String {
    return when {
        status == RoomStatus.FINISHED || seconds <= 0 ->
            recordedTimeText(finishedAtEpochMillis, "기록 없음")
        status == RoomStatus.WAITING -> "시작 후 표시"
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
    SimpleDateFormat("a h:mm:ss", Locale.KOREA).format(Date(epochMillis))

private fun adjustmentLabel(seconds: Int): String =
    if (seconds < 60) "${seconds}초" else "${seconds / 60}분"

fun timerColor(seconds: Int, status: RoomStatus, isMaintenance: Boolean = false): Color {
    return when {
        isMaintenance -> Color(0xFF64B5F6)
        status == RoomStatus.FINISHED || seconds <= 0 -> Color(0xFFFF4B4B)
        seconds <= 5 * 60 -> Color(0xFFFF4B4B)
        seconds <= 10 * 60 -> Color(0xFFFFA726)
        else -> Color(0xFF42E66F)
    }
}
