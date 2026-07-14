package com.example.escaperoomdisplay

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.KeyboardOptions
import com.example.escaperoomdisplay.alarm.DisplayGameEndAlarmController
import com.example.escaperoomdisplay.network.DisplaySyncManager
import com.example.escaperoomshared.network.TcpProtocol
import com.example.escaperoomdisplay.settings.DisplayAdminPreferences
import com.example.escaperoomdisplay.settings.DisplayAlarmSettingsDialog
import com.example.escaperoomdisplay.ui.theme.EscapeRoomTimerTheme
import com.example.escaperoomdisplay.util.openHintApp
import com.example.escaperoomshared.model.SharedRoomState

private const val ADMIN_TITLE_TAP_REQUIRED_COUNT = 10
private const val ADMIN_TITLE_TAP_TIMEOUT_MILLIS = 5_000L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        DisplaySyncManager.start(applicationContext)

        setContent {
            EscapeRoomTimerTheme {
                DisplayApp()
            }
        }
    }

}

@Composable
private fun DisplayApp() {
    val context = LocalContext.current
    val rooms by DisplaySyncManager.rooms
    val selectedRoomId by DisplaySyncManager.selectedRoomId
    val selectedRoom by DisplaySyncManager.selectedRoom
    val selectedRoomReceivedAtElapsedRealtime by DisplaySyncManager.selectedRoomReceivedAtElapsedRealtime
    val debugDemoActive by DisplaySyncManager.debugDemoActive
    val serverHost by DisplaySyncManager.serverHost
    val tcpConnected by DisplaySyncManager.isConnected

    if (selectedRoomId == null) {
        RoomSelectionScreen(
            rooms = rooms,
            debugDemoActive = debugDemoActive,
            serverHost = serverHost,
            tcpConnected = tcpConnected,
            onServerHostChanged = { host -> DisplaySyncManager.setServerHost(context, host) },
            onReconnect = DisplaySyncManager::reconnect,
            onSelectRoom = { roomId -> DisplaySyncManager.selectRoom(context, roomId) },
            onStartDebugDemo = DisplaySyncManager::startDebugDemo,
            onStopDebugDemo = DisplaySyncManager::stopDebugDemo
        )
    } else {
        GuestDisplayScreen(
            room = selectedRoom,
            roomReceivedAtElapsedRealtime = selectedRoomReceivedAtElapsedRealtime,
            tcpConnected = tcpConnected,
            debugDemoActive = debugDemoActive,
            onStartRoom = DisplaySyncManager::requestStart,
            onChangeRoom = { DisplaySyncManager.clearSelectedRoom(context) },
            onStopDebugDemo = {
                DisplaySyncManager.stopDebugDemo()
                DisplaySyncManager.clearSelectedRoom(context)
            }
        )
    }
}

@Composable
private fun RoomSelectionScreen(
    rooms: List<SharedRoomState>,
    debugDemoActive: Boolean,
    serverHost: String,
    tcpConnected: Boolean,
    onServerHostChanged: (String) -> Unit,
    onReconnect: () -> Unit,
    onSelectRoom: (String) -> Unit,
    onStartDebugDemo: () -> Unit,
    onStopDebugDemo: () -> Unit
) {
    val context = LocalContext.current
    val showDebugTools = isDebugBuild(context)
    var hostInput by remember(serverHost) { mutableStateOf(serverHost) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "손님용 태블릿 설정",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        ConnectionStatusIndicator(
            connected = tcpConnected,
            label = if (tcpConnected) "직원용 앱 연결됨" else "직원용 앱을 기다리는 중",
            debugMode = debugDemoActive,
            fontSize = 17
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = hostInput,
            onValueChange = { hostInput = it },
            label = { Text("직원용 기기 IP 주소") },
            placeholder = { Text("예: 192.168.0.15") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    Log.i("DisplayConnection", "connect button clicked: host=${hostInput.trim()}, port=${TcpProtocol.PORT}")
                    onServerHostChanged(hostInput)
                },
                modifier = Modifier.weight(1f),
                enabled = hostInput.trim().isNotEmpty()
            ) {
                Text("연결")
            }
            OutlinedButton(
                onClick = onReconnect,
                modifier = Modifier.weight(1f),
                enabled = serverHost.isNotBlank()
            ) {
                Text("다시 연결")
            }
        }

        Text(
            text = if (serverHost.isBlank()) "직원용 앱의 IP 주소를 입력해 주세요." else "저장된 주소: $serverHost",
            color = Color(0xFFB7AEC2),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(20.dp))

        if (rooms.isEmpty()) {
            Text(
                text = "두 기기를 같은 Wi-Fi에 연결하고\n직원용 앱을 실행해 주세요.",
                color = Color(0xFFD0C8D9),
                fontSize = 19.sp,
                lineHeight = 25.sp
            )
        } else {
            Text(
                text = "이 태블릿에서 사용할 방을 선택하세요.",
                color = Color(0xFFD0C8D9),
                fontSize = 17.sp
            )

            Spacer(Modifier.height(18.dp))

            rooms.forEach { room ->
                RoomSelectionCard(room = room, onClick = { onSelectRoom(room.id) })
                Spacer(Modifier.height(12.dp))
            }
        }

        if (showDebugTools) {
            Spacer(Modifier.height(24.dp))

            if (debugDemoActive) {
                OutlinedButton(
                    onClick = onStopDebugDemo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("테스트 데이터 종료")
                }
            } else {
                Button(
                    onClick = onStartDebugDemo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E2B86))
                ) {
                    Text(
                        text = "테스트 데이터 불러오기",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "이 버튼은 Debug 빌드에서만 표시됩니다.",
                color = Color(0xFFB7AEC2),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ConnectionStatusIndicator(
    connected: Boolean,
    label: String,
    debugMode: Boolean = false,
    fontSize: Int = 18
) {
    val statusColor = when {
        debugMode -> Color(0xFF9C6ADE)
        connected -> Color(0xFF44D17A)
        else -> Color(0xFFFF4B4B)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val strokeWidth = 2.6.dp.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            drawArc(
                color = statusColor,
                startAngle = 215f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(size.width * 0.08f, size.height * 0.08f),
                size = Size(size.width * 0.84f, size.height * 0.84f),
                style = stroke
            )
            drawArc(
                color = statusColor,
                startAngle = 215f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(size.width * 0.25f, size.height * 0.30f),
                size = Size(size.width * 0.50f, size.height * 0.50f),
                style = stroke
            )
            drawCircle(
                color = statusColor,
                radius = size.minDimension * 0.075f,
                center = Offset(size.width / 2f, size.height * 0.82f)
            )

            if (!connected && !debugMode) {
                drawLine(
                    color = statusColor,
                    start = Offset(size.width * 0.18f, size.height * 0.16f),
                    end = Offset(size.width * 0.84f, size.height * 0.86f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        Text(
            text = if (debugMode) "디버그 테스트 모드" else label,
            color = statusColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoomSelectionCard(room: SharedRoomState, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191621))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = room.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusLabel(room),
                    color = Color(0xFFD0C8D9),
                    fontSize = 15.sp
                )
            }

            Text(
                text = formatTime(room.seconds),
                color = if (room.seconds <= 5 * 60) Color(0xFFFF4B4B) else Color(0xFFFFB000),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GuestDisplayScreen(
    room: SharedRoomState?,
    roomReceivedAtElapsedRealtime: Long,
    tcpConnected: Boolean,
    debugDemoActive: Boolean,
    onStartRoom: (String) -> Boolean,
    onChangeRoom: () -> Unit,
    onStopDebugDemo: () -> Unit
) {
    val context = LocalContext.current
    val showDebugTools = isDebugBuild(context)
    val alarmActive by DisplayGameEndAlarmController.isActive
    var startRequestPending by remember(room?.id) { mutableStateOf(false) }
    var titleTapCount by remember { mutableStateOf(0) }
    var firstTitleTapAt by remember { mutableLongStateOf(0L) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showAdminMenu by remember { mutableStateOf(false) }
    var showPinChangeDialog by remember { mutableStateOf(false) }
    var showAlarmSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(room?.isRunning) {
        if (room?.isRunning == true) startRequestPending = false
    }

    BackHandler(enabled = true) {
        // Prevent guests from leaving the display screen accidentally.
    }

    val displayedSeconds by produceState(
        initialValue = room?.seconds ?: 0,
        key1 = room?.id,
        key2 = room?.seconds,
        key3 = roomReceivedAtElapsedRealtime
    ) {
        val snapshot = room
        if (snapshot == null) {
            value = 0
            return@produceState
        }

        while (true) {
            value = if (snapshot.isRunning && roomReceivedAtElapsedRealtime > 0L) {
                val elapsedSeconds = (
                    SystemClock.elapsedRealtime() - roomReceivedAtElapsedRealtime
                ).coerceAtLeast(0L) / 1_000L
                (snapshot.seconds - elapsedSeconds.toInt()).coerceAtLeast(0)
            } else {
                snapshot.seconds.coerceAtLeast(0)
            }

            if (!snapshot.isRunning || value <= 0) break
            val elapsedMillis = (
                SystemClock.elapsedRealtime() - roomReceivedAtElapsedRealtime
            ).coerceAtLeast(0L)
            delay((1_000L - elapsedMillis % 1_000L).coerceAtLeast(50L))
        }
    }

    val isConnected = debugDemoActive || tcpConnected
    val roomName = room?.name ?: "선택한 방을 기다리는 중"
    val isGameFinished = room != null && displayedSeconds <= 0 && !room.isRunning
    val showStartButton = room?.status == "WAITING" && !room.isRunning && !isGameFinished
    val timeText = room?.let { formatTime(displayedSeconds) } ?: "--:--"
    val timeColor = when {
        !isConnected -> Color(0xFFD0C8D9)
        displayedSeconds <= 5 * 60 -> Color(0xFFFF4B4B)
        else -> Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ConnectionStatusIndicator(
            connected = isConnected,
            label = if (isConnected) "직원용 앱 연결됨" else "연결 끊김",
            debugMode = debugDemoActive,
            fontSize = 18
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = roomName,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
                val tappedAt = System.currentTimeMillis()
                if (firstTitleTapAt == 0L || tappedAt - firstTitleTapAt > ADMIN_TITLE_TAP_TIMEOUT_MILLIS) {
                    firstTitleTapAt = tappedAt
                    titleTapCount = 1
                } else {
                    titleTapCount += 1
                }

                if (titleTapCount >= ADMIN_TITLE_TAP_REQUIRED_COUNT) {
                    titleTapCount = 0
                    firstTitleTapAt = 0L
                    showPinDialog = true
                }
            }
        )

        Spacer(Modifier.height(22.dp))

        if (showStartButton) {
            Button(
                onClick = {
                    val roomId = room?.id ?: return@Button
                    if (onStartRoom(roomId)) startRequestPending = true
                },
                enabled = isConnected && !startRequestPending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F3CC3))
            ) {
                Text(
                    text = if (startRequestPending) "시작 요청 중..." else "START",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = if (isConnected) {
                    "START를 누르면 직원용 타이머가 시작됩니다."
                } else {
                    "직원용 앱과 연결되면 START를 누를 수 있습니다."
                },
                color = Color(0xFFD0C8D9),
                fontSize = 15.sp
            )
        } else {
            Text(
                text = timeText,
                color = timeColor,
                fontSize = 88.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = when {
                    isGameFinished -> "게임 종료"
                    !isConnected -> "직원용 앱과 연결을 확인해 주세요."
                    room == null -> "선택한 방의 상태를 기다리는 중입니다."
                    room.isRunning -> "게임 진행 중"
                    else -> statusLabel(room)
                },
                color = if (isGameFinished) Color(0xFFFF4B4B) else Color(0xFFD0C8D9),
                fontSize = if (isGameFinished) 22.sp else 14.sp,
                fontWeight = if (isGameFinished) FontWeight.Bold else FontWeight.Normal
            )

            if (isGameFinished && alarmActive) {
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = DisplayGameEndAlarmController::stop,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("알람 끄기")
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = { openHintApp(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E2B86))
        ) {
            Text(
                text = "힌트 앱 열기",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(18.dp))

        if (showDebugTools && debugDemoActive) {
            Text(
                text = "테스트 종료",
                color = Color(0xFF9C6ADE),
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable(onClick = onStopDebugDemo)
                    .padding(10.dp)
            )
        }
    }

    if (showPinDialog) {
        AdminPinDialog(
            onDismiss = { showPinDialog = false },
            onVerified = {
                showPinDialog = false
                showAdminMenu = true
            }
        )
    }

    if (showAdminMenu) {
        AdminMenuDialog(
            onDismiss = { showAdminMenu = false },
            onChangeRoom = {
                showAdminMenu = false
                onChangeRoom()
            },
            onChangePin = {
                showAdminMenu = false
                showPinChangeDialog = true
            },
            onAlarmSettings = {
                showAdminMenu = false
                showAlarmSettingsDialog = true
            }
        )
    }

    if (showPinChangeDialog) {
        ChangeAdminPinDialog(
            onDismiss = { showPinChangeDialog = false },
            onSaved = { showPinChangeDialog = false }
        )
    }

    if (showAlarmSettingsDialog) {
        DisplayAlarmSettingsDialog(
            onDismiss = { showAlarmSettingsDialog = false }
        )
    }
}

@Composable
private fun AdminPinDialog(
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("관리자 PIN") },
        text = {
            Column {
                Text("관리자 설정을 열려면 PIN을 입력하세요.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        pin = value.filter(Char::isDigit).take(8)
                        errorText = null
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorText != null
                )
                errorText?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (DisplayAdminPreferences.verifyPin(context, pin)) {
                        onVerified()
                    } else {
                        errorText = "PIN이 올바르지 않습니다."
                    }
                },
                enabled = pin.isNotEmpty()
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@Composable
private fun AdminMenuDialog(
    onDismiss: () -> Unit,
    onChangeRoom: () -> Unit,
    onChangePin: () -> Unit,
    onAlarmSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("관리자 설정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onChangeRoom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("방 변경")
                }
                OutlinedButton(
                    onClick = onAlarmSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("알람 설정")
                }
                OutlinedButton(
                    onClick = onChangePin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("관리자 PIN 변경")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}

@Composable
private fun ChangeAdminPinDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("관리자 PIN 변경") },
        text = {
            Column {
                Text("4~8자리 숫자로 설정하세요.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        newPin = it.filter(Char::isDigit).take(8)
                        errorText = null
                    },
                    label = { Text("새 PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        confirmPin = it.filter(Char::isDigit).take(8)
                        errorText = null
                    },
                    label = { Text("새 PIN 확인") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorText != null
                )
                errorText?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    errorText = when {
                        newPin.length !in 4..8 -> "PIN은 4~8자리 숫자여야 합니다."
                        newPin != confirmPin -> "두 PIN이 서로 다릅니다."
                        else -> null
                    }
                    if (errorText == null) {
                        DisplayAdminPreferences.setPin(context, newPin)
                        onSaved()
                    }
                }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

private fun statusLabel(room: SharedRoomState): String {
    return when {
        room.isRunning -> "진행 중"
        room.status == "FINISHED" -> "종료"
        room.status == "WAITING" -> "대기"
        else -> "일시정지"
    }
}

private fun formatTime(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

private fun isDebugBuild(context: Context): Boolean {
    return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
