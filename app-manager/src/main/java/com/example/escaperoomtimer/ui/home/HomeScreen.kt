package com.example.escaperoomtimer.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.network.ManagerTcpServer
import com.example.escaperoomtimer.settings.StoreInfoPreferences
import com.example.escaperoomtimer.ui.common.ManagerStatusColors
import com.example.escaperoomtimer.ui.theme.AppText
import com.example.escaperoomtimer.ui.theme.AppTextSecondary
import com.example.escaperoomtimer.util.formatTime
import com.example.escaperoomtimer.util.localIpv4Address
import com.example.escaperoomtimer.web.ManagerWebServer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DashboardBorder = Color(0xFF343A40)
private val DashboardSurface = Color(0xFF101417)

@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    rooms: List<RoomInfo>,
    onRoomClick: (RoomInfo) -> Unit,
    onRoomAction: (RoomInfo) -> Unit,
    onRoomReset: (RoomInfo) -> Unit,
    onSettingsClick: () -> Unit,
    onServerClick: () -> Unit,
    onAddRoom: (name: String, defaultMinutes: Int) -> String
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentDateTime by remember { mutableStateOf(Date()) }
    var localIp by remember { mutableStateOf(localIpv4Address()) }
    var resetRoom by remember { mutableStateOf<RoomInfo?>(null) }
    var showAppInfo by remember { mutableStateOf(false) }
    val storeDisplayName = remember(context) {
        StoreInfoPreferences.load(context).displayName
            .takeUnless { it.isBlank() || it == "매장명 미설정" }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = Date()
            localIp = localIpv4Address()
            delay(30_000L)
        }
    }

    val connected = localIp != "IP 확인 불가"
    val serverStatus = when {
        !connected -> ServerStatus.DISCONNECTED
        ManagerTcpServer.isRunning && ManagerWebServer.isRunning -> ServerStatus.CONNECTED
        else -> ServerStatus.CONNECTING
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DashboardSurface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Text(
                    "EscapeRoom Suite",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                )
                NavigationDrawerItem(
                    label = { Text("운영 대시보드") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("설정") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSettingsClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("앱 정보") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAppInfo = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            DashboardHeader(
                date = formatDashboardDate(currentDateTime),
                time = formatDashboardTime(currentDateTime),
                connected = connected,
                storeName = storeDisplayName,
                onMenuClick = { scope.launch { drawerState.open() } }
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "theme-summary", span = { GridItemSpan(maxLineSpan) }) {
                    ThemeSummary(rooms = rooms)
                }
                item(key = "server-status", span = { GridItemSpan(maxLineSpan) }) {
                    ServerInfoCard(status = serverStatus, onClick = onServerClick)
                }
                if (rooms.isEmpty()) {
                    item(key = "empty-rooms", span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("사용 중인 테마가 없습니다.", color = AppText, fontWeight = FontWeight.Bold)
                            Text(
                                "설정의 테마 관리에서 테마를 추가할 수 있습니다.",
                                color = AppTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(rooms, key = { it.id }) { room ->
                        RoomCard(
                            room = room,
                            onTimerClick = { onRoomClick(room) },
                            onResetClick = { resetRoom = room },
                            onActionClick = {
                                if (room.status != RoomStatus.FINISHED && room.seconds > 0) {
                                    onRoomAction(room)
                                }
                            }
                        )
                    }
                }
                item(key = "navigation-inset", span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    }

    resetRoom?.let { room ->
        AlertDialog(
            onDismissRequest = { resetRoom = null },
            title = { Text("초기화") },
            text = {
                Text(
                    "남은 시간을 ${formatTime(room.defaultSeconds)}으로 되돌릴까요?\n" +
                        "시작 및 종료 시간 기록도 초기화됩니다."
                )
            },
            dismissButton = { TextButton(onClick = { resetRoom = null }) { Text("취소") } },
            confirmButton = {
                TextButton(onClick = {
                    onRoomReset(room)
                    resetRoom = null
                }) { Text("초기화") }
            }
        )
    }
    if (showAppInfo) {
        AlertDialog(
            onDismissRequest = { showAppInfo = false },
            title = { Text("앱 정보") },
            text = { Text("EscapeRoom Suite · 직원용") },
            confirmButton = { TextButton(onClick = { showAppInfo = false }) { Text("확인") } }
        )
    }
}

@Composable
private fun DashboardHeader(
    date: String,
    time: String,
    connected: Boolean,
    storeName: String?,
    onMenuClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(start = 8.dp, top = 4.dp, end = 16.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(48.dp).semantics { contentDescription = "메뉴 열기" }
            ) { Text("☰", color = AppText, fontSize = 28.sp) }
            Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                Text(
                    "운영 대시보드",
                    color = AppText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (storeName != null) {
                    Text(
                        storeName,
                        color = AppTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(date, color = AppTextSecondary, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(time, color = AppText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(if (connected) ManagerStatusColors.Connected else ManagerStatusColors.Disconnected)
            }
            Text(
                if (connected) "연결됨" else "연결 안 됨",
                color = AppText,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun ThemeSummary(rooms: List<RoomInfo>) {
    val running = rooms.count { it.isRunning && it.seconds > 0 }
    val finished = rooms.count { it.status == RoomStatus.FINISHED || it.seconds <= 0 }
    val paused = rooms.count {
        !it.isRunning && it.seconds > 0 && (it.status == RoomStatus.PAUSED || it.status == RoomStatus.WARNING)
    }
    val waiting = (rooms.size - running - paused - finished).coerceAtLeast(0)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("테마 현황", color = AppText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("전체 ${rooms.size}", color = AppTextSecondary, fontSize = 14.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryStatus("진행", running, ManagerStatusColors.Running)
            SummaryStatus("일시정지", paused, ManagerStatusColors.Paused)
            SummaryStatus("대기", waiting, ManagerStatusColors.Waiting)
            SummaryStatus("종료", finished, ManagerStatusColors.Finished)
        }
    }
}

@Composable
private fun SummaryStatus(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(7.dp)) { drawCircle(color) }
        Text("$label $count", color = AppText, fontSize = 12.sp, modifier = Modifier.padding(start = 5.dp))
    }
}

private enum class ServerStatus(val label: String, val color: Color) {
    CONNECTED("연결됨", ManagerStatusColors.Running),
    CONNECTING("연결 중", ManagerStatusColors.Paused),
    DISCONNECTED("연결 끊김", ManagerStatusColors.Disconnected)
}

@Composable
private fun ServerInfoCard(status: ServerStatus, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, DashboardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("서버 상태", color = AppText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Canvas(modifier = Modifier.size(8.dp)) { drawCircle(status.color) }
        Text(status.label, color = AppText, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
        Text("›", color = AppText, fontSize = 25.sp, modifier = Modifier.padding(start = 12.dp))
    }
}

private fun formatDashboardDate(date: Date): String = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(date)
private fun formatDashboardTime(date: Date): String = SimpleDateFormat("HH:mm", Locale.KOREA).format(date)
