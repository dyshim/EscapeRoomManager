package com.example.escaperoomtimer.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.network.ManagerTcpServer
import com.example.escaperoomtimer.ui.common.AddRoomDialog
import com.example.escaperoomtimer.util.localIpv4Address
import com.example.escaperoomtimer.util.nowText
import com.example.escaperoomtimer.web.ManagerWebServer
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    rooms: List<RoomInfo>,
    onRoomClick: (RoomInfo) -> Unit,
    onSettingsClick: () -> Unit,
    onAddRoom: (name: String, defaultMinutes: Int) -> String
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(nowText()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var localIp by remember { mutableStateOf(localIpv4Address()) }
    var webServerStatus by remember { mutableStateOf(ManagerWebServer.statusText()) }
    val uiPreferences = remember {
        context.getSharedPreferences("manager_home_ui", android.content.Context.MODE_PRIVATE)
    }
    var serverInfoExpanded by remember {
        mutableStateOf(uiPreferences.getBoolean("server_info_expanded", false))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = nowText()
            localIp = localIpv4Address()
            webServerStatus = ManagerWebServer.statusText()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("☰", color = Color.White, fontSize = 32.sp)
            Text("방탈출 운영", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "⚙",
                color = Color.White,
                fontSize = 27.sp,
                modifier = Modifier.clickable(onClick = onSettingsClick)
            )
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFF343B42))
        Spacer(Modifier.height(10.dp))

        CurrentTimeAndConnectionRow(
            currentTime = currentTime,
            connected = localIp != "IP 확인 불가"
        )

        Spacer(Modifier.height(10.dp))

        val guestCount = ManagerTcpServer.connectedDisplayCounts.values.sum()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF46515A), RoundedCornerShape(12.dp))
                .clickable {
                    serverInfoExpanded = !serverInfoExpanded
                    uiPreferences.edit()
                        .putBoolean("server_info_expanded", serverInfoExpanded)
                        .apply()
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("서버 정보", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "손님 ${guestCount}대 · 웹 ${ManagerWebServer.connectedWebCount}대",
                        color = Color(0xFFABB3BA),
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (serverInfoExpanded) "⌃" else "⌄",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }

            if (serverInfoExpanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF343B42))
                Spacer(Modifier.height(8.dp))
                ServerInfoLine("손님용 연결 주소", "$localIp:45991", Color(0xFF74C98C))
                val webAddress = if (localIp == "IP 확인 불가") "Wi-Fi 연결을 확인하세요" else "http://$localIp:${ManagerWebServer.PORT}"
                ServerInfoLine("PC 웹 주소", webAddress, Color(0xFF8DB9D8))
                ServerInfoLine(
                    "웹 서버",
                    webServerStatus,
                    if (ManagerWebServer.isRunning) Color(0xFF74C98C) else Color(0xFFD6A84A)
                )
                ServerInfoLine("웹 초기 PIN", "1234", Color(0xFFABB3BA))
            }
        }

        Spacer(Modifier.height(12.dp))

        if (rooms.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("사용 중인 방이 없습니다.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("아래 버튼으로 방을 추가하거나 설정에서 방을 활성화하세요.", color = Color(0xFFC8D0D7), fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rooms, key = { it.id }) { room ->
                    RoomCard(
                        room = room,
                        connectedDisplays = ManagerTcpServer.connectedCount(room.id),
                        onClick = { onRoomClick(room) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                showAddDialog = true
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF74C98C)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Text("+", color = Color(0xFF74C98C), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "새 방 추가",
                color = Color(0xFF74C98C),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    if (showAddDialog) {
        AddRoomDialog(
            suggestedName = "ROOM ${rooms.size + 1}",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, minutes ->
                onAddRoom(name, minutes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CurrentTimeAndConnectionRow(
    currentTime: String,
    connected: Boolean
) {
    val date = currentTime.substringBefore(' ')
    val time = currentTime.substringAfter(' ', currentTime)
    val connectionColor = if (connected) Color(0xFF74C98C) else Color(0xFFE57373)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(date, color = Color(0xFF969EA5), fontSize = 13.sp)
            Text(
                time,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeWifiIcon(color = connectionColor)
            Text(
                if (connected) "연결됨" else "연결 끊김",
                color = connectionColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HomeWifiIcon(color: Color) {
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
    }
}

@Composable
private fun ServerInfoLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFABB3BA), fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
