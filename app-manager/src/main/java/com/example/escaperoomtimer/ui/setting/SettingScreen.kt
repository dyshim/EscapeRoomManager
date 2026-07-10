package com.example.escaperoomtimer.ui.setting

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.ThemePreset
import com.example.escaperoomtimer.repository.ThemePresetRepository
import java.util.UUID

@Composable
fun SettingScreen(
    rooms: List<RoomInfo>,
    onBack: () -> Unit,
    onSaveRoom: (roomId: String, name: String, defaultMinutes: Int) -> Unit,
    onSetRoomEnabled: (roomId: String, enabled: Boolean) -> Boolean,
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

    var presetName by remember { mutableStateOf("") }
    var presetMinutes by remember { mutableStateOf("60") }
    var presetEmoji by remember { mutableStateOf("🎭") }
    var editingPresetId by remember { mutableStateOf<String?>(null) }
    var roomForPresetDialog by remember { mutableStateOf<RoomInfo?>(null) }
    var roomToDelete by remember { mutableStateOf<RoomInfo?>(null) }
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var newRoomMinutes by remember { mutableStateOf("60") }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F12))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", color = Color.White, fontSize = 28.sp, modifier = Modifier.clickable(onClick = onBack))
            Text("방·테마 설정", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("", fontSize = 15.sp)
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFF2A2F35))
        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ManagerAlarmSettingsSection() }

            item {
                Text("테마 프리셋", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("테마 이름과 기본 시간을 저장해 방에 빠르게 적용할 수 있어요.", color = Color(0xFF8D96A0), fontSize = 13.sp)
            }

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
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF171C20)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("아직 저장된 테마 프리셋이 없어요.", color = Color(0xFF9AA3AC), modifier = Modifier.padding(16.dp))
                    }
                }
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

            item {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Color(0xFF2A2F35))
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("방 관리", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("사용 여부, 순서, 이름과 기본 시간을 관리합니다.", color = Color(0xFF8D96A0), fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            newRoomName = "ROOM ${rooms.size + 1}"
                            newRoomMinutes = "60"
                            showAddRoomDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00))
                    ) { Text("+ 추가") }
                }
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
                    onMoveUp = { onMoveRoom(room.id, -1) },
                    onMoveDown = { onMoveRoom(room.id, 1) },
                    onDelete = { roomToDelete = room }
                )
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
        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            title = { Text("새 방 추가") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = { newRoomName = it },
                        label = { Text("방 이름") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRoomMinutes,
                        onValueChange = { newRoomMinutes = it.filter(Char::isDigit).take(3) },
                        label = { Text("기본 시간(분)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = newRoomMinutes.toIntOrNull()?.coerceIn(1, 240)
                    if (newRoomName.trim().isBlank() || minutes == null) {
                        Toast.makeText(context, "방 이름과 시간을 확인해 주세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        onAddRoom(newRoomName, minutes)
                        showAddRoomDialog = false
                    }
                }) { Text("추가") }
            },
            dismissButton = { TextButton(onClick = { showAddRoomDialog = false }) { Text("취소") } }
        )
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
