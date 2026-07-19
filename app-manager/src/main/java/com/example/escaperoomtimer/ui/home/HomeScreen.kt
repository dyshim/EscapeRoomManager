package com.example.escaperoomtimer.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.network.ManagerTcpServer
import com.example.escaperoomtimer.ui.common.ManagerStatusColors
import com.example.escaperoomtimer.util.localIpv4Address
import com.example.escaperoomtimer.util.nowText
import com.example.escaperoomtimer.web.ManagerWebServer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val DashboardBorder = Color(0xFF3A4147)
private val DashboardMuted = Color(0xFFB5BBC1)

@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    rooms: List<RoomInfo>,
    onRoomClick: (RoomInfo) -> Unit,
    onSettingsClick: () -> Unit,
    onAddRoom: (name: String, defaultMinutes: Int) -> String
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(nowText()) }
    var localIp by remember { mutableStateOf(localIpv4Address()) }
    val uiPreferences = remember {
        context.getSharedPreferences("manager_home_ui", Context.MODE_PRIVATE)
    }
    var serverInfoExpanded by remember {
        mutableStateOf(uiPreferences.getBoolean("server_info_expanded", true))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = nowText()
            localIp = localIpv4Address()
        }
    }

    val guestCount = ManagerTcpServer.connectedDisplayCounts.values.sum()
    val webCount = ManagerWebServer.connectedWebCount
    val connected = localIp != "IP 확인 불가"
    val tcpAddress = if (connected) "$localIp:45991" else "Wi-Fi 연결을 확인하세요"
    val webAddress = if (connected) "http://$localIp:${ManagerWebServer.PORT}" else "Wi-Fi 연결을 확인하세요"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            DashboardTopBar(onSettingsClick = onSettingsClick)
        }
        item {
            CurrentTimeCard(currentTime = currentTime, connected = connected)
        }
        item {
            ServerInfoCard(
                expanded = serverInfoExpanded,
                guestCount = guestCount,
                webCount = webCount,
                tcpAddress = tcpAddress,
                webAddress = webAddress,
                tcpRunning = connected,
                webRunning = ManagerWebServer.isRunning,
                onToggle = {
                    serverInfoExpanded = !serverInfoExpanded
                    uiPreferences.edit().putBoolean("server_info_expanded", serverInfoExpanded).apply()
                },
                onCopy = { label, value -> copyToClipboard(context, label, value) }
            )
        }
        item {
            ThemeStatusCard(
                rooms = rooms,
                onRoomClick = onRoomClick
            )
        }
    }
}

@Composable
private fun DashboardTopBar(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("☰", color = Color.White, fontSize = 30.sp)
        Text(
            "운영 대시보드",
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 22.dp).weight(1f)
        )
        Text(
            "⚙",
            color = Color.White,
            fontSize = 27.sp,
            modifier = Modifier.clickable(onClick = onSettingsClick).padding(5.dp)
        )
    }
}

@Composable
private fun CurrentTimeCard(currentTime: String, connected: Boolean) {
    val time = currentTime.substringAfter(' ', currentTime)
    val color = if (connected) ManagerStatusColors.Connected else ManagerStatusColors.Disconnected
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DashboardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(formatDashboardDate(), color = Color.White, fontSize = 14.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(time, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                HomeWifiIcon(color = color)
                Text(
                    if (connected) "연결됨" else "연결 안 됨",
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun ServerInfoCard(
    expanded: Boolean,
    guestCount: Int,
    webCount: Int,
    tcpAddress: String,
    webAddress: String,
    tcpRunning: Boolean,
    webRunning: Boolean,
    onToggle: () -> Unit,
    onCopy: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DashboardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("서버 정보", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("손님 ${guestCount}대 · 웹 ${webCount}대", color = DashboardMuted, fontSize = 13.sp)
            Text(if (expanded) "⌃" else "⌄", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
        }
        if (expanded) {
            Spacer(Modifier.height(9.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DashboardBorder, RoundedCornerShape(7.dp))
            ) {
                ServerAddressRow(
                    label = "손님용 TCP",
                    address = tcpAddress,
                    running = tcpRunning,
                    onCopy = { onCopy("손님용 TCP", tcpAddress) }
                )
                HorizontalDivider(color = DashboardBorder)
                ServerAddressRow(
                    label = "웹 대시보드",
                    address = webAddress,
                    running = webRunning,
                    onCopy = { onCopy("웹 대시보드", webAddress) }
                )
            }
        }
    }
}

@Composable
private fun ServerAddressRow(
    label: String,
    address: String,
    running: Boolean,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(43.dp).padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(if (running) ManagerStatusColors.Connected else ManagerStatusColors.Disconnected)
        }
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(start = 9.dp))
        Text(
            address,
            color = DashboardMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp).weight(1f)
        )
        CopyIcon(
            modifier = Modifier.size(28.dp).clickable(onClick = onCopy).padding(5.dp),
            color = Color.White
        )
    }
}

@Composable
private fun ThemeStatusCard(
    rooms: List<RoomInfo>,
    onRoomClick: (RoomInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DashboardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("테마 현황", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${rooms.size}개", color = DashboardMuted, fontSize = 16.sp)
        }
        HorizontalDivider(color = DashboardBorder, modifier = Modifier.padding(top = 5.dp))
        if (rooms.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("사용 중인 테마가 없습니다.", color = Color.White, fontSize = 16.sp)
                Text("설정의 테마 관리에서 테마를 추가할 수 있습니다.", color = DashboardMuted, fontSize = 12.sp)
            }
        } else {
            rooms.forEachIndexed { index, room ->
                RoomCard(
                    room = room,
                    connectedDisplays = ManagerTcpServer.connectedCount(room.id),
                    onClick = { onRoomClick(room) }
                )
                if (index < rooms.lastIndex) {
                    HorizontalDivider(color = DashboardBorder)
                }
            }
        }
    }
}

@Composable
private fun HomeWifiIcon(color: Color) {
    Canvas(modifier = Modifier.size(width = 20.dp, height = 17.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawArc(color, 215f, 110f, false, Offset(size.width * .05f, 0f), Size(size.width * .9f, size.height * .9f), style = stroke)
        drawArc(color, 215f, 110f, false, Offset(size.width * .28f, size.height * .32f), Size(size.width * .44f, size.height * .44f), style = stroke)
        drawCircle(color, radius = size.minDimension * .07f, center = Offset(size.width / 2f, size.height * .88f))
    }
}

@Composable
private fun CopyIcon(modifier: Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.5.dp.toPx())
        drawRect(color, topLeft = Offset(size.width * .12f, size.height * .08f), size = Size(size.width * .62f, size.height * .68f), style = stroke)
        drawRect(color, topLeft = Offset(size.width * .28f, size.height * .25f), size = Size(size.width * .62f, size.height * .68f), style = stroke)
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    if (value == "Wi-Fi 연결을 확인하세요") {
        Toast.makeText(context, "Wi-Fi 연결 후 주소를 복사해 주세요.", Toast.LENGTH_SHORT).show()
        return
    }

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, "$label 주소를 복사했습니다.", Toast.LENGTH_SHORT).show()
}

private fun formatDashboardDate(): String =
    SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREA).format(Date())
