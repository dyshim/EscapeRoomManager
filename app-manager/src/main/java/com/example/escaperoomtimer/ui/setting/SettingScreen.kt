package com.example.escaperoomtimer.ui.setting

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.ThemePreset
import com.example.escaperoomtimer.network.ManagerTcpServer
import com.example.escaperoomtimer.repository.ThemePresetRepository
import com.example.escaperoomtimer.settings.WebAdminPinPreferences
import com.example.escaperoomtimer.ui.common.AddRoomDialog
import com.example.escaperoomtimer.util.localIpv4Address
import com.example.escaperoomtimer.web.ManagerWebServer
import java.util.UUID
import kotlinx.coroutines.delay

private enum class SettingPage(val title: String, val subtitle: String) {
    MENU("설정", "매장 운영 옵션을 관리합니다."),
    ALARM("알림 및 소리", "타이머 종료 알림을 설정합니다."),
    WEB_PIN("웹 관리자 PIN", "PC 웹 로그인 PIN을 관리합니다."),
    PRESETS("테마 프리셋", "방 이름과 기본 시간 조합을 관리합니다."),
    ROOMS("방 관리", "방의 사용 여부와 기본 설정을 관리합니다."),
    SERVER("서버 및 연결", "손님용 TCP와 웹 서버 상태를 확인합니다.")
}

private enum class SettingIcon { BELL, SHIELD, PALETTE, DOOR, SERVER }

@Composable
fun SettingScreen(
    rooms: List<RoomInfo>,
    onBack: () -> Unit,
    onSaveRoom: (roomId: String, name: String, defaultMinutes: Int) -> Unit,
    onSetRoomEnabled: (roomId: String, enabled: Boolean) -> Boolean,
    onSetMaintenance: (roomId: String, maintenance: Boolean) -> Boolean,
    onDeleteRoom: (roomId: String) -> Boolean,
    onMoveRoom: (roomId: String, direction: Int) -> Boolean,
    onAddRoom: (name: String, defaultMinutes: Int) -> String
) {
    val context = LocalContext.current
    val nameInputs = remember { mutableStateMapOf<String, String>() }
    val minuteInputs = remember { mutableStateMapOf<String, String>() }
    val presets = remember {
        mutableStateListOf<ThemePreset>().apply { addAll(ThemePresetRepository.load(context)) }
    }
    var currentPage by remember { mutableStateOf(SettingPage.MENU) }
    var localIp by remember { mutableStateOf(localIpv4Address()) }
    var webServerStatus by remember { mutableStateOf(ManagerWebServer.statusText()) }

    var presetName by remember { mutableStateOf("") }
    var presetMinutes by remember { mutableStateOf("60") }
    var presetEmoji by remember { mutableStateOf("🎭") }
    var editingPresetId by remember { mutableStateOf<String?>(null) }
    var roomForPresetDialog by remember { mutableStateOf<RoomInfo?>(null) }
    var roomToDelete by remember { mutableStateOf<RoomInfo?>(null) }
    var showAddRoomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(rooms.map { it.id to it.name }) {
        rooms.forEach { room ->
            if (nameInputs[room.id] == null) nameInputs[room.id] = room.name
            if (minuteInputs[room.id] == null) minuteInputs[room.id] = room.defaultMinutes.toString()
        }
        val ids = rooms.mapTo(mutableSetOf()) { it.id }
        nameInputs.keys.toList().filterNot(ids::contains).forEach { nameInputs.remove(it) }
        minuteInputs.keys.toList().filterNot(ids::contains).forEach { minuteInputs.remove(it) }
    }

    fun persistPresets() = ThemePresetRepository.save(context, presets)
    fun clearPresetEditor() {
        editingPresetId = null
        presetName = ""
        presetMinutes = "60"
        presetEmoji = "🎭"
    }

    LaunchedEffect(currentPage) {
        if (currentPage == SettingPage.SERVER) {
            while (true) {
                localIp = localIpv4Address()
                webServerStatus = ManagerWebServer.statusText()
                delay(1000)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F12))
            .padding(18.dp)
    ) {
        SettingTopBar(
            page = currentPage,
            onBack = {
                if (currentPage == SettingPage.MENU) onBack() else currentPage = SettingPage.MENU
            }
        )

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFF2A2F35))
        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (currentPage) {
                SettingPage.MENU -> {
                    item {
                        SettingsMenu(
                            presetCount = presets.size,
                            roomCount = rooms.size,
                            onPageSelected = { currentPage = it }
                        )
                    }
                }
                SettingPage.ALARM -> item { ManagerAlarmSettingsSection() }
                SettingPage.WEB_PIN -> item { WebAdminPinSettingsSection() }
                SettingPage.PRESETS -> {
                    item {
                        PresetEditorCard(
                            presetName = presetName,
                            presetMinutes = presetMinutes,
                            presetEmoji = presetEmoji,
                            isEditing = editingPresetId != null,
                            onNameChange = { presetName = it },
                            onMinutesChange = { presetMinutes = it.filter(Char::isDigit).take(3) },
                            onEmojiChange = { presetEmoji = it.take(4) },
                            onSave = {
                                val cleanName = presetName.trim()
                                val minutes = presetMinutes.toIntOrNull()?.coerceIn(1, 240)
                                if (cleanName.isBlank() || minutes == null) {
                                    Toast.makeText(context, "테마 이름과 시간을 확인해 주세요.", Toast.LENGTH_SHORT).show()
                                    return@PresetEditorCard
                                }
                                val existingIndex = presets.indexOfFirst { it.id == editingPresetId }
                                val preset = ThemePreset(
                                    id = editingPresetId ?: UUID.randomUUID().toString(),
                                    name = cleanName,
                                    defaultMinutes = minutes,
                                    emoji = presetEmoji.ifBlank { "🎭" }
                                )
                                if (existingIndex >= 0) presets[existingIndex] = preset else presets.add(preset)
                                persistPresets()
                                clearPresetEditor()
                            },
                            onCancel = ::clearPresetEditor
                        )
                    }
                    if (presets.isEmpty()) {
                        item { EmptyPresetCard() }
                    } else {
                        items(presets, key = { it.id }) { preset ->
                            PresetCard(
                                preset = preset,
                                onEdit = {
                                    editingPresetId = preset.id
                                    presetName = preset.name
                                    presetMinutes = preset.defaultMinutes.toString()
                                    presetEmoji = preset.emoji
                                },
                                onDelete = {
                                    presets.removeAll { it.id == preset.id }
                                    persistPresets()
                                    if (editingPresetId == preset.id) clearPresetEditor()
                                }
                            )
                        }
                    }
                }
                SettingPage.ROOMS -> {
                    item {
                        Button(
                            onClick = { showAddRoomDialog = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF74C98C),
                                contentColor = Color(0xFF07130B)
                            )
                        ) { Text("+ 새 방 추가", fontWeight = FontWeight.Bold) }
                    }
                    items(rooms, key = { it.id }) { room ->
                        RoomSettingCard(
                            room = room,
                            nameValue = nameInputs[room.id] ?: room.name,
                            minutesValue = minuteInputs[room.id] ?: room.defaultMinutes.toString(),
                            hasPresets = presets.isNotEmpty(),
                            onNameChange = { nameInputs[room.id] = it },
                            onMinutesChange = { minuteInputs[room.id] = it.filter(Char::isDigit).take(3) },
                            onChoosePreset = { roomForPresetDialog = room },
                            onSave = {
                                val minutes = minuteInputs[room.id]?.toIntOrNull() ?: room.defaultMinutes
                                val name = nameInputs[room.id] ?: room.name
                                onSaveRoom(room.id, name, minutes)
                                Toast.makeText(context, "${name.trim()} 저장 완료", Toast.LENGTH_SHORT).show()
                            },
                            onEnabledChange = { enabled ->
                                if (!onSetRoomEnabled(room.id, enabled)) {
                                    Toast.makeText(context, "진행 중인 방은 비활성화할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onMaintenanceChange = { maintenance ->
                                if (!onSetMaintenance(room.id, maintenance)) {
                                    Toast.makeText(context, "진행 중인 방은 유지보수로 전환할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onMoveUp = { onMoveRoom(room.id, -1) },
                            onMoveDown = { onMoveRoom(room.id, 1) },
                            onDelete = { roomToDelete = room }
                        )
                    }
                }
                SettingPage.SERVER -> item {
                    ServerConnectionCard(localIp = localIp, webServerStatus = webServerStatus)
                }
            }
        }
    }

    roomForPresetDialog?.let { room ->
        AlertDialog(
            onDismissRequest = { roomForPresetDialog = null },
            title = { Text("${room.name}에 적용할 테마") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets, key = { it.id }) { preset ->
                        OutlinedButton(
                            onClick = {
                                nameInputs[room.id] = preset.name
                                minuteInputs[room.id] = preset.defaultMinutes.toString()
                                onSaveRoom(room.id, preset.name, preset.defaultMinutes)
                                roomForPresetDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("${preset.emoji} ${preset.name} · ${preset.defaultMinutes}분") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { roomForPresetDialog = null }) { Text("닫기") } }
        )
    }

    roomToDelete?.let { room ->
        AlertDialog(
            onDismissRequest = { roomToDelete = null },
            title = { Text("방 삭제") },
            text = { Text("${room.name}을(를) 삭제할까요? 저장된 타이머 상태도 함께 삭제됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    if (onDeleteRoom(room.id)) {
                        Toast.makeText(context, "${room.name} 삭제 완료", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "진행 중인 방 또는 마지막 남은 방은 삭제할 수 없습니다.", Toast.LENGTH_LONG).show()
                    }
                    roomToDelete = null
                }) { Text("삭제", color = Color(0xFFFF4B4B)) }
            },
            dismissButton = { TextButton(onClick = { roomToDelete = null }) { Text("취소") } }
        )
    }

    if (showAddRoomDialog) {
        AddRoomDialog(
            suggestedName = "ROOM ${rooms.size + 1}",
            onDismiss = { showAddRoomDialog = false },
            onConfirm = { name, minutes ->
                onAddRoom(name, minutes)
                showAddRoomDialog = false
            }
        )
    }
}

@Composable
private fun SettingTopBar(page: SettingPage, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            "←",
            color = Color.White,
            fontSize = 28.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 4.dp)
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(page.title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(page.subtitle, color = Color(0xFF8D96A0), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsMenu(
    presetCount: Int,
    roomCount: Int,
    onPageSelected: (SettingPage) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingMenuCard(
            title = "알림 및 소리",
            description = "타이머 종료 알림과 소리를 설정합니다.",
            color = Color(0xFFF0BD55),
            icon = SettingIcon.BELL,
            onClick = { onPageSelected(SettingPage.ALARM) }
        )
        SettingMenuCard(
            title = "웹 관리자 PIN",
            description = "PC 웹 로그인 PIN을 변경합니다.",
            color = Color(0xFF75B8F3),
            icon = SettingIcon.SHIELD,
            badge = "설정됨",
            onClick = { onPageSelected(SettingPage.WEB_PIN) }
        )
        SettingMenuCard(
            title = "테마 프리셋",
            description = "저장된 테마와 기본 시간을 관리합니다.",
            color = Color(0xFFBF91EA),
            icon = SettingIcon.PALETTE,
            badge = "${presetCount}개",
            onClick = { onPageSelected(SettingPage.PRESETS) }
        )
        SettingMenuCard(
            title = "방 관리",
            description = "방 이름, 시간과 사용 여부를 관리합니다.",
            color = Color(0xFF79D892),
            icon = SettingIcon.DOOR,
            badge = "${roomCount}개",
            onClick = { onPageSelected(SettingPage.ROOMS) }
        )
        SettingMenuCard(
            title = "서버 및 연결",
            description = "손님용 TCP와 웹 서버 정보를 확인합니다.",
            color = Color(0xFF73D5C8),
            icon = SettingIcon.SERVER,
            onClick = { onPageSelected(SettingPage.SERVER) }
        )
        Text(
            "EscapeRoom Suite · 직원용",
            color = Color(0xFF727A82),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun SettingMenuCard(
    title: String,
    description: String,
    color: Color,
    icon: SettingIcon,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF3A4650)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151B20))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingMenuIcon(icon = icon, color = color)
            Column(modifier = Modifier.weight(1f).padding(start = 18.dp)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(description, color = Color(0xFFB2BAC2), fontSize = 12.sp, maxLines = 1)
            }
            badge?.let {
                Text(
                    text = it,
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            Text("›", color = Color.White, fontSize = 30.sp, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

@Composable
private fun SettingMenuIcon(icon: SettingIcon, color: Color) {
    Canvas(modifier = Modifier.size(48.dp)) {
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (icon) {
            SettingIcon.BELL -> {
                val bell = Path().apply {
                    moveTo(w * .25f, h * .68f)
                    lineTo(w * .34f, h * .55f)
                    lineTo(w * .34f, h * .36f)
                    cubicTo(w * .34f, h * .18f, w * .66f, h * .18f, w * .66f, h * .36f)
                    lineTo(w * .66f, h * .55f)
                    lineTo(w * .75f, h * .68f)
                    close()
                }
                drawPath(bell, color, style = stroke)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(w * .44f, h * .75f), end = androidx.compose.ui.geometry.Offset(w * .56f, h * .75f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            SettingIcon.SHIELD -> {
                val shield = Path().apply {
                    moveTo(w * .5f, h * .14f)
                    lineTo(w * .76f, h * .25f)
                    lineTo(w * .72f, h * .58f)
                    cubicTo(w * .68f, h * .75f, w * .56f, h * .82f, w * .5f, h * .85f)
                    cubicTo(w * .44f, h * .82f, w * .32f, h * .75f, w * .28f, h * .58f)
                    lineTo(w * .24f, h * .25f)
                    close()
                }
                drawPath(shield, color, style = stroke)
                drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * .40f, h * .43f), size = androidx.compose.ui.geometry.Size(w * .20f, h * .20f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .03f), style = stroke)
            }
            SettingIcon.PALETTE -> {
                drawCircle(color, radius = w * .31f, center = center, style = stroke)
                drawCircle(Color(0xFF151B20), radius = w * .10f, center = androidx.compose.ui.geometry.Offset(w * .69f, h * .67f))
                listOf(.35f to .35f, .55f to .28f, .30f to .55f).forEach { (x, y) ->
                    drawCircle(color, radius = w * .035f, center = androidx.compose.ui.geometry.Offset(w * x, h * y))
                }
            }
            SettingIcon.DOOR -> {
                drawRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * .30f, h * .18f), size = androidx.compose.ui.geometry.Size(w * .42f, h * .65f), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .30f, h * .18f), androidx.compose.ui.geometry.Offset(w * .56f, h * .27f), stroke.width, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .56f, h * .27f), androidx.compose.ui.geometry.Offset(w * .56f, h * .77f), stroke.width, StrokeCap.Round)
                drawCircle(color, radius = w * .025f, center = androidx.compose.ui.geometry.Offset(w * .50f, h * .53f))
            }
            SettingIcon.SERVER -> {
                repeat(3) { index ->
                    val top = h * (.20f + index * .22f)
                    drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * .24f, top), size = androidx.compose.ui.geometry.Size(w * .52f, h * .16f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .04f), style = stroke)
                    drawCircle(color, radius = w * .022f, center = androidx.compose.ui.geometry.Offset(w * .32f, top + h * .08f))
                }
            }
        }
    }
}

@Composable
private fun EmptyPresetCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171C20)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text("아직 저장된 테마 프리셋이 없어요.", color = Color(0xFF9AA3AC), modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun ServerConnectionCard(localIp: String, webServerStatus: String) {
    val guestCount = ManagerTcpServer.connectedDisplayCounts.values.sum()
    val validIp = localIp != "IP 확인 불가"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF31554F)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151B20))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("현재 서버 상태", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("주소는 확인용이며 이 화면에서는 변경할 수 없습니다.", color = Color(0xFF9AA3AC), fontSize = 13.sp)
            HorizontalDivider(color = Color(0xFF344048))
            ServerInfoRow("Wi-Fi", if (validIp) "연결됨" else "연결 안 됨", if (validIp) Color(0xFF79D892) else Color(0xFFE57373))
            ServerInfoRow("손님용 TCP", if (validIp) "$localIp:45991" else "주소 확인 불가", Color(0xFF73D5C8))
            ServerInfoRow("웹 대시보드", if (validIp) "http://$localIp:${ManagerWebServer.PORT}" else "주소 확인 불가", Color(0xFF75B8F3))
            ServerInfoRow("연결 기기", "손님 ${guestCount}대 · 웹 ${ManagerWebServer.connectedWebCount}대", Color.White)
            ServerInfoRow("웹 서버", webServerStatus, if (ManagerWebServer.isRunning) Color(0xFF79D892) else Color(0xFFF0BD55))
        }
    }
}

@Composable
private fun ServerInfoRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFAAB2BA), fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PresetEditorCard(
    presetName: String,
    presetMinutes: String,
    presetEmoji: String,
    isEditing: Boolean,
    onNameChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171C20))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(if (isEditing) "프리셋 수정" else "새 프리셋 추가", color = Color(0xFFFFB000), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = presetEmoji, onValueChange = onEmojiChange, modifier = Modifier.weight(0.28f), singleLine = true, label = { Text("아이콘") })
                OutlinedTextField(value = presetName, onValueChange = onNameChange, modifier = Modifier.weight(0.72f), singleLine = true, label = { Text("테마 이름") })
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = presetMinutes,
                onValueChange = onMinutesChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("기본 시간(분)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00))) {
                    Text(if (isEditing) "수정 저장" else "프리셋 추가")
                }
                if (isEditing) OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("취소") }
            }
        }
    }
}

@Composable
private fun PresetCard(preset: ThemePreset, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171C20))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("${preset.emoji} ${preset.name}", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("기본 ${preset.defaultMinutes}분", color = Color(0xFF9AA3AC), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("수정") }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("삭제", color = Color(0xFFFF6B6B)) }
            }
        }
    }
}

@Composable
private fun RoomSettingCard(
    room: RoomInfo,
    nameValue: String,
    minutesValue: String,
    hasPresets: Boolean,
    onNameChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onChoosePreset: () -> Unit,
    onSave: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onMaintenanceChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (room.isEnabled) Color(0xFF171C20) else Color(0xFF111417)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(room.id.uppercase(), color = Color(0xFFFFB000), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(if (room.isEnabled) "사용 중" else "사용 안 함", color = if (room.isEnabled) Color(0xFF44D17A) else Color(0xFF8D96A0), fontSize = 12.sp)
                }
                Switch(checked = room.isEnabled, onCheckedChange = onEnabledChange, enabled = !room.isRunning)
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("유지보수 모드", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("손님용 방 선택에서 숨기고 타이머 시작을 막습니다.", color = Color(0xFF9AA3AC), fontSize = 12.sp)
                }
                Switch(
                    checked = room.isMaintenance,
                    onCheckedChange = onMaintenanceChange,
                    enabled = room.isEnabled && !room.isRunning
                )
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onChoosePreset, enabled = hasPresets, modifier = Modifier.fillMaxWidth()) {
                Text(if (hasPresets) "테마 프리셋 적용" else "먼저 프리셋을 추가하세요")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = nameValue, onValueChange = onNameChange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("방 이름") })
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = minutesValue,
                onValueChange = onMinutesChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("기본 시간(분)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00))
            ) { Text("저장", color = Color.White, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onMoveUp, modifier = Modifier.weight(1f)) { Text("↑ 위") }
                OutlinedButton(onClick = onMoveDown, modifier = Modifier.weight(1f)) { Text("↓ 아래") }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), enabled = !room.isRunning) {
                    Text("삭제", color = Color(0xFFFF6B6B))
                }
            }
        }
    }
}


@Composable
private fun WebAdminPinSettingsSection() {
    val context = LocalContext.current
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171C20)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("웹 관리자 PIN", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(
                "PC 웹 로그인에 사용하는 숫자 PIN입니다. 최초 PIN은 1234입니다.",
                color = Color(0xFFB8C0C8),
                fontSize = 13.sp
            )

            OutlinedTextField(
                value = currentPin,
                onValueChange = { currentPin = it.filter(Char::isDigit).take(8) },
                label = { Text("현재 PIN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newPin,
                onValueChange = { newPin = it.filter(Char::isDigit).take(8) },
                label = { Text("새 PIN (4~8자리)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { confirmPin = it.filter(Char::isDigit).take(8) },
                label = { Text("새 PIN 확인") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        when {
                            !WebAdminPinPreferences.isValidPin(newPin) ->
                                Toast.makeText(context, "새 PIN은 숫자 4~8자리로 입력해 주세요.", Toast.LENGTH_SHORT).show()
                            newPin != confirmPin ->
                                Toast.makeText(context, "새 PIN 확인이 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                            WebAdminPinPreferences.changePin(context, currentPin, newPin) -> {
                                currentPin = ""
                                newPin = ""
                                confirmPin = ""
                                Toast.makeText(context, "웹 관리자 PIN을 변경했습니다.", Toast.LENGTH_SHORT).show()
                            }
                            else -> Toast.makeText(context, "현재 PIN이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("PIN 변경") }

                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.weight(1f)
                ) { Text("1234로 초기화") }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("웹 PIN 초기화") },
            text = { Text("웹 관리자 PIN을 1234로 초기화할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    WebAdminPinPreferences.resetToDefault(context)
                    currentPin = ""
                    newPin = ""
                    confirmPin = ""
                    showResetDialog = false
                    Toast.makeText(context, "웹 관리자 PIN을 1234로 초기화했습니다.", Toast.LENGTH_SHORT).show()
                }) { Text("초기화", color = Color(0xFFFF7A00)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("취소") }
            }
        )
    }
}
