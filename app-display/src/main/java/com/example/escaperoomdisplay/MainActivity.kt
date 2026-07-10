package com.example.escaperoomdisplay

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.escaperoomdisplay.alarm.DisplayGameEndAlarmController
import com.example.escaperoomdisplay.network.DisplaySyncManager
import com.example.escaperoomdisplay.settings.DisplayAdminPreferences
import com.example.escaperoomdisplay.ui.theme.EscapeRoomTimerTheme
import com.example.escaperoomdisplay.util.openHintApp
import com.example.escaperoomshared.model.SharedRoomState
import kotlinx.coroutines.delay

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

    override fun onDestroy() {
        DisplaySyncManager.stop()
        super.onDestroy()
    }
}

@Composable
private fun DisplayApp() {
    val context = LocalContext.current
    val rooms by DisplaySyncManager.rooms
    val selectedRoomId by DisplaySyncManager.selectedRoomId
    val selectedRoom by DisplaySyncManager.selectedRoom
    val lastReceivedAt by DisplaySyncManager.lastReceivedAtMillis
    val debugDemoActive by DisplaySyncManager.debugDemoActive
    val serverHost by DisplaySyncManager.serverHost
    val tcpConnected by DisplaySyncManager.isConnected

    if (selectedRoomId == null) {
        RoomSelectionScreen(
            rooms = rooms,
            lastReceivedAtMillis = lastReceivedAt,
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
            lastReceivedAtMillis = lastReceivedAt,
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
    lastReceivedAtMillis: Long,
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
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }

    val isConnected = lastReceivedAtMillis > 0L && now - lastReceivedAtMillis <= 5_000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080B0E))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "손님용 태블릿 설정",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = when {
                debugDemoActive -> "디버그 테스트 모드"
                tcpConnected && isConnected -> "직원용 앱 연결됨"
                else -> "직원용 앱을 기다리는 중"
            },
            color = when {
                debugDemoActive -> Color(0xFF9C6ADE)
                tcpConnected && isConnected -> Color(0xFF44D17A)
                else -> Color(0xFFFFB000)
            },
            fontSize = 15.sp
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
                onClick = { onServerHostChanged(hostInput) },
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
            color = Color(0xFF687078),
            fontSize = 12.sp
        )

        Spacer(Modifier.height(20.dp))

        if (rooms.isEmpty()) {
            Text(
                text = "두 기기를 같은 Wi-Fi에 연결하고\n직원용 앱을 실행해 주세요.",
                color = Color(0xFF9AA4AD),
                fontSize = 17.sp,
                lineHeight = 25.sp
            )
        } else {
            Text(
                text = "이 태블릿에서 사용할 방을 선택하세요.",
                color = Color(0xFF9AA4AD),
                fontSize = 15.sp
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
                        .height(54.dp),
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
                color = Color(0xFF687078),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RoomSelectionCard(room: SharedRoomState, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171C20))
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusLabel(room),
                    color = Color(0xFF9AA4AD),
                    fontSize = 13.sp
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
    lastReceivedAtMillis: Long,
    debugDemoActive: Boolean,
    onStartRoom: (String) -> Boolean,
    onChangeRoom: () -> Unit,
    onStopDebugDemo: () -> Unit
) {
    val context = LocalContext.current
    val showDebugTools = isDebugBuild(context)
    val alarmActive by DisplayGameEndAlarmController.isActive
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var startRequestPending by remember(room?.id) { mutableStateOf(false) }
    var titleTapCount by remember { mutableStateOf(0) }
    var firstTitleTapAt by remember { mutableLongStateOf(0L) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showAdminMenu by remember { mutableStateOf(false) }
    var showPinChangeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(room?.isRunning) {
        if (room?.isRunning == true) startRequestPending = false
    }

    BackHandler(enabled = true) {
        // Prevent guests from leaving the display screen accidentally.
    }

    val isConnected = debugDemoActive ||
        (lastReceivedAtMillis > 0L && now - lastReceivedAtMillis <= 5_000L)
    val roomName = room?.name ?: "선택한 방을 기다리는 중"
    val isGameFinished = room != null && room.seconds <= 0 && !room.isRunning
    val showStartButton = room != null && !room.isRunning && !isGameFinished
    val timeText = room?.seconds?.let(::formatTime) ?: "--:--"
    val timeColor = when {
        !isConnected -> Color(0xFF9AA4AD)
        (room?.seconds ?: Int.MAX_VALUE) <= 5 * 60 -> Color(0xFFFF4B4B)
        else -> Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080B0E))
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when {
                debugDemoActive -> "디버그 테스트 모드"
                isConnected -> "직원용 앱 연결됨"
                else -> "연결 끊김"
            },
            color = when {
                debugDemoActive -> Color(0xFF9C6ADE)
                isConnected -> Color(0xFF44D17A)
                else -> Color(0xFFFF4B4B)
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = roomName,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
                val tappedAt = System.currentTimeMillis()
                if (firstTitleTapAt == 0L || tappedAt - firstTitleTapAt > 3_000L) {
                    firstTitleTapAt = tappedAt
                    titleTapCount = 1
                } else {
                    titleTapCount += 1
                }

                if (titleTapCount >= 5) {
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
                    .height(76.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F3CC3))
            ) {
                Text(
                    text = if (startRequestPending) "시작 요청 중..." else "START",
                    color = Color.White,
                    fontSize = 28.sp,
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
                color = Color(0xFF9AA4AD),
                fontSize = 13.sp
            )
        } else {
            Text(
                text = timeText,
                color = timeColor,
                fontSize = 82.sp,
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
                color = if (isGameFinished) Color(0xFFFF4B4B) else Color(0xFF9AA4AD),
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(18.dp))

        if (showDebugTools && debugDemoActive) {
            Text(
                text = "테스트 종료",
                color = Color(0xFF9C6ADE),
                fontSize = 13.sp,
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
            }
        )
    }

    if (showPinChangeDialog) {
        ChangeAdminPinDialog(
            onDismiss = { showPinChangeDialog = false },
            onSaved = { showPinChangeDialog = false }
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
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
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
    onChangePin: () -> Unit
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
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
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
