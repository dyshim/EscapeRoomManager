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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomdisplay.network.DisplaySyncManager
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

    if (selectedRoomId == null) {
        RoomSelectionScreen(
            rooms = rooms,
            lastReceivedAtMillis = lastReceivedAt,
            debugDemoActive = debugDemoActive,
            onSelectRoom = { roomId -> DisplaySyncManager.selectRoom(context, roomId) },
            onStartDebugDemo = DisplaySyncManager::startDebugDemo,
            onStopDebugDemo = DisplaySyncManager::stopDebugDemo
        )
    } else {
        GuestDisplayScreen(
            room = selectedRoom,
            lastReceivedAtMillis = lastReceivedAt,
            debugDemoActive = debugDemoActive,
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
    onSelectRoom: (String) -> Unit,
    onStartDebugDemo: () -> Unit,
    onStopDebugDemo: () -> Unit
) {
    val context = LocalContext.current
    val showDebugTools = isDebugBuild(context)
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
                isConnected -> "직원용 앱 연결됨"
                else -> "직원용 앱을 기다리는 중"
            },
            color = when {
                debugDemoActive -> Color(0xFF9C6ADE)
                isConnected -> Color(0xFF44D17A)
                else -> Color(0xFFFFB000)
            },
            fontSize = 15.sp
        )

        Spacer(Modifier.height(28.dp))

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
    onChangeRoom: () -> Unit,
    onStopDebugDemo: () -> Unit
) {
    val context = LocalContext.current
    val showDebugTools = isDebugBuild(context)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }

    BackHandler(enabled = true) {
        // Prevent guests from leaving the display screen accidentally.
    }

    val isConnected = lastReceivedAtMillis > 0L && now - lastReceivedAtMillis <= 5_000L
    val roomName = room?.name ?: "선택한 방을 기다리는 중"
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
                isConnected -> "손님용 화면"
                else -> "연결 끊김"
            },
            color = when {
                debugDemoActive -> Color(0xFF9C6ADE)
                isConnected -> Color(0xFFFFB000)
                else -> Color(0xFFFF4B4B)
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = roomName,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = timeText,
            color = timeColor,
            fontSize = 82.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = when {
                debugDemoActive -> "가짜 타이머가 1초마다 감소하는 중"
                !isConnected -> "직원용 앱과 연결을 확인해 주세요."
                room == null -> "선택한 방의 상태를 기다리는 중입니다."
                else -> "직원용 앱과 실시간 동기화 중"
            },
            color = Color(0xFF9AA4AD),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(44.dp))

        Button(
            onClick = { openHintApp(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E2B86))
        ) {
            Text(
                text = "힌트앱 열기",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "방 변경",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier
                .clickable(onClick = onChangeRoom)
                .padding(10.dp)
        )

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
