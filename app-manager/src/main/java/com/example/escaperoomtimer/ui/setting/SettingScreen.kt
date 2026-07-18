package com.example.escaperoomtimer.ui.setting

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.ThemePreset
import com.example.escaperoomtimer.network.ManagerTcpServer
import com.example.escaperoomtimer.repository.ThemePresetRepository
import com.example.escaperoomtimer.settings.ManagerAlarmPreferences
import com.example.escaperoomtimer.settings.BackupHistoryItem
import com.example.escaperoomtimer.settings.ManagerBackup
import com.example.escaperoomtimer.settings.ManagerBackupManager
import com.example.escaperoomtimer.settings.StoreInfo
import com.example.escaperoomtimer.settings.StoreInfoPreferences
import com.example.escaperoomtimer.settings.WebAdminPinPreferences
import com.example.escaperoomtimer.ui.common.AddRoomDialog
import com.example.escaperoomtimer.ui.theme.AppBlack
import com.example.escaperoomtimer.ui.theme.AppSurface
import com.example.escaperoomtimer.util.localIpv4Address
import com.example.escaperoomtimer.web.ManagerWebServer
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private enum class SettingPage(val title: String, val subtitle: String) {
    MENU("설정", "매장 운영 옵션을 관리합니다."),
    STORE("매장 정보", "앱과 백업 파일에 사용할 매장 정보를 설정합니다."),
    ALARM("알림 및 소리", "타이머 종료 알림을 설정합니다."),
    WEB_PIN("웹 관리자 PIN", "PC 웹 로그인 PIN을 관리합니다."),
    PRESETS("테마 프리셋", "테마 이름과 기본 시간 조합을 관리합니다."),
    ROOMS("테마 관리", "테마의 사용 여부와 기본 설정을 관리합니다."),
    BACKUP("백업 및 복원", "직원용 앱의 설정을 저장하거나 다시 불러옵니다."),
    BACKUP_CREATE("설정 백업", "현재 저장된 설정을 하나의 백업 파일로 만듭니다."),
    BACKUP_RESTORE("설정 복원", "백업 파일을 확인한 뒤 설정을 복원합니다."),
    SERVER("서버 및 연결", "손님용 TCP와 웹 서버 상태를 확인합니다.")
}

private enum class SettingIcon { STORE, BELL, SHIELD, PALETTE, DOOR, BACKUP, SERVER, POWER }

@Composable
fun SettingScreen(
    rooms: List<RoomInfo>,
    onBack: () -> Unit,
    onSaveRoom: (roomId: String, name: String, defaultMinutes: Int) -> Unit,
    onSetRoomEnabled: (roomId: String, enabled: Boolean) -> Boolean,
    onSetMaintenance: (roomId: String, maintenance: Boolean) -> Boolean,
    onDeleteRoom: (roomId: String) -> Boolean,
    onMoveRoom: (roomId: String, direction: Int) -> Boolean,
    onAddRoom: (name: String, defaultMinutes: Int) -> String,
    onRestoreRooms: (List<RoomInfo>) -> Boolean,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val nameInputs = remember { mutableStateMapOf<String, String>() }
    val minuteInputs = remember { mutableStateMapOf<String, String>() }
    val presets = remember {
        mutableStateListOf<ThemePreset>().apply { addAll(ThemePresetRepository.load(context)) }
    }
    var currentPage by remember { mutableStateOf(SettingPage.MENU) }
    var savedStoreInfo by remember { mutableStateOf(StoreInfoPreferences.load(context)) }
    var storeName by remember { mutableStateOf(savedStoreInfo.storeName) }
    var branchName by remember { mutableStateOf(savedStoreInfo.branchName) }
    var storeInfoSaved by remember { mutableStateOf(false) }
    var showDiscardStoreChanges by remember { mutableStateOf(false) }
    var showResetStoreInfo by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<ManagerBackup?>(null) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var backupHistory by remember { mutableStateOf(ManagerBackupManager.history(context)) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backupError by remember { mutableStateOf<String?>(null) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var exitInProgress by remember { mutableStateOf(false) }
    var localIp by remember { mutableStateOf(localIpv4Address()) }
    var webServerStatus by remember { mutableStateOf(ManagerWebServer.statusText()) }

    var presetName by remember { mutableStateOf("") }
    var presetMinutes by remember { mutableStateOf("60") }
    var editingPresetId by remember { mutableStateOf<String?>(null) }
    var showPresetEditor by remember { mutableStateOf(false) }
    var reorderPresets by remember { mutableStateOf(false) }
    var roomForPresetDialog by remember { mutableStateOf<RoomInfo?>(null) }
    var roomToDelete by remember { mutableStateOf<RoomInfo?>(null) }
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    var reorderRooms by remember { mutableStateOf(false) }
    var showAddRoomDialog by remember { mutableStateOf(false) }
    val storeInfoChanged = storeName.trim() != savedStoreInfo.storeName ||
        branchName.trim() != savedStoreInfo.branchName
    val backupFileName = remember(savedStoreInfo) {
        val storePart = listOf(savedStoreInfo.storeName, savedStoreInfo.branchName)
            .filter { it.isNotBlank() }
            .joinToString("_")
            .replace(Regex("[^가-힣A-Za-z0-9_-]"), "_")
            .trim('_')
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.KOREA).format(Date())
        if (storePart.isBlank()) "EscapeRoom_Backup_$stamp.ers" else "EscapeRoom_${storePart}_$stamp.ers"
    }
    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) runCatching {
            ManagerBackupManager.write(
                context,
                uri,
                ManagerBackup(
                    createdAtEpochMillis = System.currentTimeMillis(),
                    storeInfo = StoreInfoPreferences.load(context),
                    rooms = rooms,
                    presets = presets.toList(),
                    alarmSettings = ManagerAlarmPreferences.load(context)
                )
            )
        }.onSuccess {
            val currentBackup = ManagerBackup(
                createdAtEpochMillis = System.currentTimeMillis(),
                storeInfo = StoreInfoPreferences.load(context),
                rooms = rooms,
                presets = presets.toList(),
                alarmSettings = ManagerAlarmPreferences.load(context)
            )
            ManagerBackupManager.saveInternal(context, backupFileName, currentBackup)
            backupHistory = ManagerBackupManager.history(context)
            backupError = null
            backupMessage = "백업 파일이 저장되었습니다."
        }.onFailure {
            backupMessage = null
            backupError = it.message ?: "백업 파일을 저장하지 못했습니다."
        }
    }
    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) runCatching { ManagerBackupManager.read(context, uri) }
            .onSuccess {
                backupError = null
                pendingRestore = it
                currentPage = SettingPage.BACKUP_RESTORE
            }
            .onFailure {
                backupMessage = null
                backupError = it.message ?: "백업 파일을 읽지 못했습니다."
            }
    }

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
        showPresetEditor = false
    }
    fun movePreset(index: Int, direction: Int) {
        val target = index + direction
        if (index !in presets.indices || target !in presets.indices) return
        val current = presets[index]
        presets[index] = presets[target]
        presets[target] = current
        persistPresets()
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
            .background(AppBlack)
            .padding(18.dp)
    ) {
        SettingTopBar(
            page = currentPage,
            onBack = {
                when {
                    currentPage == SettingPage.MENU -> onBack()
                    currentPage == SettingPage.STORE && storeInfoChanged -> showDiscardStoreChanges = true
                    currentPage == SettingPage.BACKUP_CREATE || currentPage == SettingPage.BACKUP_RESTORE -> currentPage = SettingPage.BACKUP
                    currentPage == SettingPage.ROOMS && selectedRoomId != null -> selectedRoomId = null
                    else -> currentPage = SettingPage.MENU
                }
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
                            storeInfoConfigured = savedStoreInfo.storeName.isNotBlank(),
                            presetCount = presets.size,
                            roomCount = rooms.size,
                            onPageSelected = { currentPage = it },
                            onExitSelected = { showExitConfirmation = true }
                        )
                    }
                }
                SettingPage.STORE -> item {
                    StoreInfoSettingsSection(
                        storeName = storeName,
                        branchName = branchName,
                        saved = storeInfoSaved,
                        changed = storeInfoChanged,
                        onStoreNameChange = {
                            storeName = it.take(30)
                            storeInfoSaved = false
                        },
                        onBranchNameChange = {
                            branchName = it.take(20)
                            storeInfoSaved = false
                        },
                        onSave = {
                            val info = StoreInfo(storeName.trim(), branchName.trim())
                            StoreInfoPreferences.save(context, info)
                            savedStoreInfo = info
                            storeName = info.storeName
                            branchName = info.branchName
                            storeInfoSaved = true
                        },
                        onReset = { showResetStoreInfo = true }
                    )
                }
                SettingPage.BACKUP -> item {
                    BackupRestoreHomeSection(
                        history = backupHistory,
                        onCreateBackup = { currentPage = SettingPage.BACKUP_CREATE },
                        onChooseRestore = {
                            backupMessage = null
                            backupError = null
                            openBackupLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                        },
                        onHistorySelected = { item ->
                            runCatching { ManagerBackupManager.readInternal(context, item.fileName) }
                                .onSuccess {
                                    pendingRestore = it
                                    currentPage = SettingPage.BACKUP_RESTORE
                                }
                                .onFailure { backupError = it.message }
                        }
                    )
                }
                SettingPage.BACKUP_CREATE -> item {
                    BackupCreateSettingsSection(
                        fileName = backupFileName,
                        restoreEnabled = rooms.none { it.isRunning },
                        message = backupMessage,
                        error = backupError,
                        onCreateBackup = {
                            backupMessage = null
                            backupError = null
                            createBackupLauncher.launch(backupFileName)
                        },
                        onCancel = { currentPage = SettingPage.BACKUP }
                    )
                }
                SettingPage.BACKUP_RESTORE -> item {
                    BackupRestorePreviewSection(
                        backup = pendingRestore,
                        currentRoomCount = rooms.size,
                        currentPresetCount = presets.size,
                        hasRunningTimer = rooms.any { it.isRunning },
                        message = backupMessage,
                        error = backupError,
                        onChooseFile = { openBackupLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                        onRestore = { showRestoreConfirmation = true }
                    )
                }
                SettingPage.ALARM -> item { ManagerAlarmSettingsSection() }
                SettingPage.WEB_PIN -> item { WebAdminPinSettingsSection() }
                SettingPage.PRESETS -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    clearPresetEditor()
                                    showPresetEditor = true
                                    reorderPresets = false
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DCE))
                            ) { Text("+ 새 프리셋", fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = {
                                    reorderPresets = !reorderPresets
                                    if (reorderPresets) clearPresetEditor()
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                border = BorderStroke(1.dp, Color(0xFF9C6ADE))
                            ) {
                                Text(if (reorderPresets) "순서 변경 완료" else "순서 변경", color = Color(0xFFBF91EA))
                            }
                        }
                    }
                    if (showPresetEditor) item {
                        PresetEditorCard(
                            presetName = presetName,
                            presetMinutes = presetMinutes,
                            isEditing = editingPresetId != null,
                            onNameChange = { presetName = it },
                            onMinutesChange = { presetMinutes = it.filter(Char::isDigit).take(3) },
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
                                    emoji = presets.getOrNull(existingIndex)?.emoji ?: "🎭"
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
                        itemsIndexed(presets, key = { _, preset -> preset.id }) { index, preset ->
                            PresetCard(
                                preset = preset,
                                ordering = reorderPresets,
                                canMoveUp = index > 0,
                                canMoveDown = index < presets.lastIndex,
                                onEdit = {
                                    editingPresetId = preset.id
                                    presetName = preset.name
                                    presetMinutes = preset.defaultMinutes.toString()
                                    showPresetEditor = true
                                },
                                onDelete = {
                                    presets.removeAll { it.id == preset.id }
                                    persistPresets()
                                    if (editingPresetId == preset.id) clearPresetEditor()
                                },
                                onMoveUp = { movePreset(index, -1) },
                                onMoveDown = { movePreset(index, 1) }
                            )
                        }
                    }
                }
                SettingPage.ROOMS -> {
                    val selectedRoom = rooms.firstOrNull { it.id == selectedRoomId }
                    if (selectedRoom == null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        reorderRooms = false
                                        showAddRoomDialog = true
                                    },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF74C98C),
                                        contentColor = Color(0xFF07130B)
                                    )
                                ) { Text("+ 새 테마", fontWeight = FontWeight.Bold) }
                                OutlinedButton(
                                    onClick = { reorderRooms = !reorderRooms },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    border = BorderStroke(1.dp, Color(0xFF74C98C))
                                ) {
                                    Text(if (reorderRooms) "순서 변경 완료" else "순서 변경", color = Color(0xFF74C98C))
                                }
                            }
                        }
                        itemsIndexed(rooms, key = { _, room -> room.id }) { index, room ->
                            RoomSummaryCard(
                                room = room,
                                ordering = reorderRooms,
                                canMoveUp = index > 0,
                                canMoveDown = index < rooms.lastIndex,
                                onClick = { selectedRoomId = room.id },
                                onEnabledChange = { enabled ->
                                    if (!onSetRoomEnabled(room.id, enabled)) {
                                        Toast.makeText(context, "진행 중인 테마는 비활성화할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onMoveUp = { onMoveRoom(room.id, -1) },
                                onMoveDown = { onMoveRoom(room.id, 1) }
                            )
                        }
                        item {
                            Text(
                                "테마를 누르면 이름, 시간, 유지보수와 삭제를 설정할 수 있습니다.",
                                color = Color(0xFF7F878E),
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        item {
                            RoomSettingCard(
                                room = selectedRoom,
                                nameValue = nameInputs[selectedRoom.id] ?: selectedRoom.name,
                                minutesValue = minuteInputs[selectedRoom.id] ?: selectedRoom.defaultMinutes.toString(),
                                hasPresets = presets.isNotEmpty(),
                                onNameChange = { nameInputs[selectedRoom.id] = it },
                                onMinutesChange = { minuteInputs[selectedRoom.id] = it.filter(Char::isDigit).take(3) },
                                onChoosePreset = { roomForPresetDialog = selectedRoom },
                                onSave = {
                                    val minutes = minuteInputs[selectedRoom.id]?.toIntOrNull() ?: selectedRoom.defaultMinutes
                                    val name = nameInputs[selectedRoom.id] ?: selectedRoom.name
                                    onSaveRoom(selectedRoom.id, name, minutes)
                                    Toast.makeText(context, "${name.trim()} 저장 완료", Toast.LENGTH_SHORT).show()
                                },
                                onEnabledChange = { enabled ->
                                    if (!onSetRoomEnabled(selectedRoom.id, enabled)) {
                                        Toast.makeText(context, "진행 중인 테마는 비활성화할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onMaintenanceChange = { maintenance ->
                                    if (!onSetMaintenance(selectedRoom.id, maintenance)) {
                                        Toast.makeText(context, "진행 중인 테마는 유지보수로 전환할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDelete = { roomToDelete = selectedRoom }
                            )
                        }
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
                        ) { Text("${preset.name} · ${preset.defaultMinutes}분") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { roomForPresetDialog = null }) { Text("닫기") } }
        )
    }

    roomToDelete?.let { room ->
        AlertDialog(
            onDismissRequest = { roomToDelete = null },
            title = { Text("테마 삭제") },
            text = { Text("${room.name}을(를) 삭제할까요? 저장된 타이머 상태도 함께 삭제됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    if (onDeleteRoom(room.id)) {
                        Toast.makeText(context, "${room.name} 삭제 완료", Toast.LENGTH_SHORT).show()
                        if (selectedRoomId == room.id) selectedRoomId = null
                    } else {
                        Toast.makeText(context, "진행 중인 테마 또는 마지막 남은 테마는 삭제할 수 없습니다.", Toast.LENGTH_LONG).show()
                    }
                    roomToDelete = null
                }) { Text("삭제", color = Color(0xFFFF4B4B)) }
            },
            dismissButton = { TextButton(onClick = { roomToDelete = null }) { Text("취소") } }
        )
    }

    if (showDiscardStoreChanges) {
        AlertDialog(
            onDismissRequest = { showDiscardStoreChanges = false },
            title = { Text("변경 내용을 저장하지 않고 나갈까요?") },
            text = { Text("입력한 변경 내용이 저장되지 않습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    storeName = savedStoreInfo.storeName
                    branchName = savedStoreInfo.branchName
                    storeInfoSaved = false
                    showDiscardStoreChanges = false
                    currentPage = SettingPage.MENU
                }) { Text("나가기", color = Color(0xFFFF5252)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiscardStoreChanges = false }) { Text("계속 편집") }
            }
        )
    }

    if (showResetStoreInfo) {
        AlertDialog(
            onDismissRequest = { showResetStoreInfo = false },
            title = { Text("매장 정보를 초기화할까요?") },
            text = { Text("운영 대시보드와 백업 파일에서 매장 정보가 제거됩니다. 테마와 타이머 설정은 유지됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    StoreInfoPreferences.clear(context)
                    savedStoreInfo = StoreInfo()
                    storeName = ""
                    branchName = ""
                    storeInfoSaved = true
                    showResetStoreInfo = false
                }) { Text("초기화", color = Color(0xFFFF5252)) }
            },
            dismissButton = { TextButton(onClick = { showResetStoreInfo = false }) { Text("취소") } }
        )
    }

    if (showRestoreConfirmation) pendingRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false },
            title = { Text("이 설정을 복원할까요?") },
            text = {
                Text(
                    "테마 ${backup.rooms.size}개 · 프리셋 ${backup.presets.size}개\n" +
                        "매장 정보: ${backup.storeInfo.displayName.ifBlank { "없음" }}\n\n" +
                        "현재 설정을 덮어쓰며 모든 테마는 대기 상태로 적용됩니다. 웹 관리자 PIN은 변경되지 않습니다."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val safetyBackup = ManagerBackup(
                        createdAtEpochMillis = System.currentTimeMillis(),
                        storeInfo = StoreInfoPreferences.load(context),
                        rooms = rooms.toList(),
                        presets = presets.toList(),
                        alarmSettings = ManagerAlarmPreferences.load(context)
                    )
                    if (onRestoreRooms(backup.rooms)) {
                        ManagerBackupManager.createSafetyBackup(context, safetyBackup)
                        ThemePresetRepository.save(context, backup.presets)
                        presets.clear()
                        presets.addAll(backup.presets)
                        StoreInfoPreferences.save(context, backup.storeInfo)
                        savedStoreInfo = backup.storeInfo
                        storeName = backup.storeInfo.storeName
                        branchName = backup.storeInfo.branchName
                        ManagerAlarmPreferences.save(context, backup.alarmSettings)
                        backupHistory = ManagerBackupManager.history(context)
                        backupError = null
                        backupMessage = "설정 복원이 완료되었습니다."
                    } else {
                        backupMessage = null
                        backupError = "실행 중인 타이머가 있어 복원할 수 없습니다."
                    }
                    showRestoreConfirmation = false
                }) { Text("복원") }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirmation = false }) { Text("취소") } }
        )
    }

    if (showExitConfirmation) {
        val runningRooms = rooms.filter { it.isRunning }
        ExitAppConfirmationDialog(
            runningRooms = runningRooms,
            guestCount = ManagerTcpServer.connectedDisplayCounts.values.sum(),
            webCount = ManagerWebServer.connectedWebCount,
            onDismiss = { showExitConfirmation = false },
            onConfirm = {
                showExitConfirmation = false
                exitInProgress = true
                onExitApp()
            }
        )
    }

    if (exitInProgress) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("앱을 안전하게 종료하는 중입니다.") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(34.dp), color = Color(0xFF74C98C))
                    Text("현재 상태를 저장하고 연결 서버를 정리합니다.")
                }
            },
            confirmButton = {}
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
    storeInfoConfigured: Boolean,
    presetCount: Int,
    roomCount: Int,
    onPageSelected: (SettingPage) -> Unit,
    onExitSelected: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingMenuCard(
            title = "매장 정보",
            description = "매장명과 지점 정보를 관리합니다.",
            color = Color(0xFFFF765C),
            icon = SettingIcon.STORE,
            badge = if (storeInfoConfigured) "설정됨" else null,
            onClick = { onPageSelected(SettingPage.STORE) }
        )
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
            title = "테마 관리",
            description = "테마 이름, 시간과 사용 여부를 관리합니다.",
            color = Color(0xFF79D892),
            icon = SettingIcon.DOOR,
            badge = "${roomCount}개",
            onClick = { onPageSelected(SettingPage.ROOMS) }
        )
        SettingMenuCard(
            title = "설정 백업 및 복원",
            description = "운영 설정을 파일로 보관하고 복원합니다.",
            color = Color(0xFF4FC3F7),
            icon = SettingIcon.BACKUP,
            onClick = { onPageSelected(SettingPage.BACKUP) }
        )
        SettingMenuCard(
            title = "서버 및 연결",
            description = "손님용 TCP와 웹 서버 정보를 확인합니다.",
            color = Color(0xFF73D5C8),
            icon = SettingIcon.SERVER,
            onClick = { onPageSelected(SettingPage.SERVER) }
        )
        Text("앱", color = Color(0xFF8D96A0), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        SettingMenuCard(
            title = "직원용 앱 종료",
            description = "직원용 앱과 연결 서버를 안전하게 종료합니다.",
            color = Color(0xFFFF5252),
            icon = SettingIcon.POWER,
            onClick = onExitSelected
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
        colors = CardDefaults.cardColors(containerColor = AppSurface)
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
            SettingIcon.STORE -> {
                drawRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * .25f, h * .38f), size = androidx.compose.ui.geometry.Size(w * .50f, h * .40f), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .20f, h * .38f), androidx.compose.ui.geometry.Offset(w * .28f, h * .20f), stroke.width, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .28f, h * .20f), androidx.compose.ui.geometry.Offset(w * .72f, h * .20f), stroke.width, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .72f, h * .20f), androidx.compose.ui.geometry.Offset(w * .80f, h * .38f), stroke.width, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .43f, h * .78f), androidx.compose.ui.geometry.Offset(w * .43f, h * .56f), stroke.width, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .43f, h * .56f), androidx.compose.ui.geometry.Offset(w * .58f, h * .56f), stroke.width, StrokeCap.Round)
            }
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
            SettingIcon.BACKUP -> {
                drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * .22f, h * .18f), size = androidx.compose.ui.geometry.Size(w * .56f, h * .64f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .06f), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .50f, h * .30f), androidx.compose.ui.geometry.Offset(w * .50f, h * .60f), stroke.width, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .38f, h * .48f), androidx.compose.ui.geometry.Offset(w * .50f, h * .60f), stroke.width, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .62f, h * .48f), androidx.compose.ui.geometry.Offset(w * .50f, h * .60f), stroke.width, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .36f, h * .70f), androidx.compose.ui.geometry.Offset(w * .64f, h * .70f), stroke.width, StrokeCap.Round)
            }
            SettingIcon.SERVER -> {
                repeat(3) { index ->
                    val top = h * (.20f + index * .22f)
                    drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * .24f, top), size = androidx.compose.ui.geometry.Size(w * .52f, h * .16f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .04f), style = stroke)
                    drawCircle(color, radius = w * .022f, center = androidx.compose.ui.geometry.Offset(w * .32f, top + h * .08f))
                }
            }
            SettingIcon.POWER -> {
                drawArc(color, startAngle = -45f, sweepAngle = 270f, useCenter = false, topLeft = androidx.compose.ui.geometry.Offset(w * .20f, h * .20f), size = androidx.compose.ui.geometry.Size(w * .60f, h * .60f), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .50f, h * .12f), androidx.compose.ui.geometry.Offset(w * .50f, h * .48f), stroke.width, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun StoreInfoSettingsSection(
    storeName: String,
    branchName: String,
    saved: Boolean,
    changed: Boolean,
    onStoreNameChange: (String) -> Unit,
    onBranchNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    val cleanStoreName = storeName.trim()
    val cleanBranchName = branchName.trim()
    val displayName = listOf(cleanStoreName, cleanBranchName)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { "표시할 매장 정보가 없습니다." }
    val safeName = listOf(cleanStoreName, cleanBranchName)
        .filter { it.isNotBlank() }
        .joinToString("_")
        .replace(Regex("[^가-힣A-Za-z0-9_-]"), "_")
        .trim('_')
    val timestamp = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.KOREA).format(Date()) }
    val backupName = if (safeName.isBlank()) {
        "EscapeRoom_Backup_${timestamp}.ers"
    } else {
        "EscapeRoom_${safeName}_${timestamp}.ers"
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF3A4650)),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = storeName,
                    onValueChange = onStoreNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("매장명") },
                    supportingText = {
                        Text(if (cleanStoreName.isBlank()) "매장명을 입력해 주세요." else "직원용 앱과 웹 대시보드에 표시됩니다.")
                    },
                    isError = cleanStoreName.isBlank(),
                    singleLine = true,
                    suffix = { Text("${storeName.length}/30") }
                )
                OutlinedTextField(
                    value = branchName,
                    onValueChange = onBranchNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("지점명 (선택)") },
                    supportingText = { Text("여러 지점을 구분할 때 사용합니다.") },
                    singleLine = true,
                    suffix = { Text("${branchName.length}/20") }
                )
                HorizontalDivider(color = Color(0xFF384049))
                Text("표시 이름 미리보기", color = Color(0xFF9AA3AC), fontSize = 12.sp)
                Text(displayName, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF11161A)),
                    border = BorderStroke(1.dp, Color(0xFF313941)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("운영 대시보드", color = Color(0xFF9AA3AC), fontSize = 11.sp)
                        Text(displayName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text("실제 백업 파일 이름", color = Color(0xFF9AA3AC), fontSize = 12.sp)
                Text(
                    backupName,
                    color = Color(0xFFB8C1C9),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("ⓘ 사용할 수 없는 문자는 파일 이름에서 자동으로 정리됩니다.", color = Color(0xFF8D96A0), fontSize = 11.sp)
                Text("ⓘ 정보를 변경해도 실행 중인 타이머와 기기 연결은 유지됩니다.", color = Color(0xFF8D96A0), fontSize = 11.sp)
                Button(
                    onClick = onSave,
                    enabled = cleanStoreName.isNotBlank() && changed,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                ) { Text("저장", fontWeight = FontWeight.Bold) }
                TextButton(onClick = onReset, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("매장 정보 초기화", color = Color(0xFFB8C1C9))
                }
            }
        }
        if (saved) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF62C900)),
                colors = CardDefaults.cardColors(containerColor = Color(0x142D6B00)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("✓  매장 정보가 저장되었습니다.", color = Color(0xFF82D72D), modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun BackupRestoreHomeSection(
    history: List<BackupHistoryItem>,
    onCreateBackup: () -> Unit,
    onChooseRestore: () -> Unit,
    onHistorySelected: (BackupHistoryItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        history.firstOrNull()?.let { recent ->
            BackupCardTitle("최근 백업")
            BackupHistoryCard(recent, onClick = { onHistorySelected(recent) })
            if (history.size > 1) {
                Text("이전 백업", color = Color(0xFFB8C1C9), fontSize = 13.sp)
                history.drop(1).forEach { BackupHistoryCard(it, onClick = { onHistorySelected(it) }) }
            }
        }
        BackupActionCard("설정 백업", "현재 설정을 백업 파일로 저장합니다.", Color(0xFF8B4DFF), onCreateBackup)
        BackupActionCard("설정 복원", "백업 파일을 확인한 뒤 설정을 복원합니다.", Color(0xFFB8C1C9), onChooseRestore)
        Text("ⓘ 웹 관리자 PIN은 백업에 포함되지 않습니다.", color = Color(0xFF9AA3AC), fontSize = 11.sp)
    }
}

@Composable
private fun BackupCreateSettingsSection(
    fileName: String,
    restoreEnabled: Boolean,
    message: String?,
    error: String?,
    onCreateBackup: () -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFF3A4650)), colors = CardDefaults.cardColors(containerColor = AppSurface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("백업 내용", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("전체 항목 · 5개", color = Color(0xFFB8C1C9), fontSize = 12.sp)
            Text("☑ 테마 설정\n☑ 테마 프리셋\n☑ 알림 및 소리\n☑ 웹 관리자 설정 · PIN 제외\n☑ 기타 앱 설정", color = Color(0xFFD8DEE4), fontSize = 13.sp)
            HorizontalDivider(color = Color(0xFF384049))
            Text("백업 파일", color = Color.White, fontWeight = FontWeight.Bold)
            Text(fileName, color = Color(0xFFB8C1C9), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("ⓘ 현재 타이머 진행 상태와 연결된 기기 정보는 포함되지 않습니다.", color = Color(0xFF9AA3AC), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(50.dp)) { Text("취소") }
                Button(onClick = onCreateBackup, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C36E8))) { Text("백업 파일 만들기", fontWeight = FontWeight.Bold) }
            }
        }
    }
    BackupResultMessages(message, error)
}

@Composable
private fun BackupRestorePreviewSection(
    backup: ManagerBackup?,
    currentRoomCount: Int,
    currentPresetCount: Int,
    hasRunningTimer: Boolean,
    message: String?,
    error: String?,
    onChooseFile: () -> Unit,
    onRestore: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFF3A4650)), colors = CardDefaults.cardColors(containerColor = AppSurface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (backup == null) "백업 파일을 선택해 주세요." else "사용 가능한 백업", color = if (backup == null) Color.White else Color(0xFF82D72D), fontWeight = FontWeight.Bold)
                backup?.let {
                    Text("✓ 파일 검사 완료\n✓ 앱 버전 호환", color = Color(0xFFB8C1C9), fontSize = 12.sp)
                }
                OutlinedButton(onClick = onChooseFile, modifier = Modifier.fillMaxWidth()) { Text(if (backup == null) "백업 파일 선택" else "다른 파일 선택") }
            }
        }
        backup?.let {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppSurface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("변경 내용", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("테마        ${currentRoomCount}개  →  ${it.rooms.size}개\n프리셋      ${currentPresetCount}개  →  ${it.presets.size}개\n알림 설정   변경됨\n웹 관리자 PIN   현재 설정 유지", color = Color(0xFFB8C1C9), fontSize = 13.sp)
                }
            }
        }
        if (hasRunningTimer) Text("⚠ 실행 중인 테마가 있습니다.\n실행 중인 테마를 모두 정지한 뒤 복원해 주세요.", color = Color(0xFFFFB000), modifier = Modifier.fillMaxWidth().background(Color(0x332F2500), RoundedCornerShape(10.dp)).padding(12.dp))
        Button(onClick = onRestore, enabled = backup != null && !hasRunningTimer, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C36E8))) { Text("설정 복원", fontWeight = FontWeight.Bold) }
        Text("ⓘ 복원 직전에 현재 설정을 안전 백업으로 자동 저장합니다.", color = Color(0xFF9AA3AC), fontSize = 11.sp)
        BackupResultMessages(message, error)
    }
}

@Composable private fun BackupCardTitle(text: String) = Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)

@Composable
private fun BackupHistoryCard(item: BackupHistoryItem, onClick: () -> Unit) {
    val date = remember(item.createdAtEpochMillis) { SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(item.createdAtEpochMillis)) }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = AppSurface), border = BorderStroke(1.dp, Color(0xFF3A4650))) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(date, color = Color.White, fontWeight = FontWeight.Bold)
            Text(item.fileName, color = Color(0xFFB8C1C9), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("테마 ${item.roomCount}개 · 프리셋 ${item.presetCount}개 · 사용 가능", color = Color(0xFF82D72D), fontSize = 12.sp)
        }
    }
}

@Composable
private fun BackupActionCard(title: String, description: String, color: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = AppSurface), border = BorderStroke(1.dp, Color(0xFF3A4650))) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⇧", color = color, fontSize = 30.sp)
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) { Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(description, color = Color(0xFFB8C1C9), fontSize = 12.sp) }
            Text("›", color = Color.White, fontSize = 30.sp)
        }
    }
}

@Composable
private fun BackupResultMessages(message: String?, error: String?) {
    message?.let { Text("✓  $it", color = Color(0xFF82D72D), modifier = Modifier.fillMaxWidth().background(Color(0x142D6B00), RoundedCornerShape(10.dp)).padding(12.dp)) }
    error?.let { Text("✕  $it", color = Color(0xFFFF6B6B), modifier = Modifier.fillMaxWidth().background(Color(0x22FF5252), RoundedCornerShape(10.dp)).padding(12.dp)) }
}

@Composable
private fun ExitAppConfirmationDialog(
    runningRooms: List<RoomInfo>,
    guestCount: Int,
    webCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val hasRunningRooms = runningRooms.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (hasRunningRooms) "실행 중인 테마 ${runningRooms.size}개" else "직원용 앱을 종료할까요?",
                color = if (hasRunningRooms) Color(0xFFFFB000) else Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (hasRunningRooms) "종료 후 직원용 앱에서는 제어할 수 없습니다."
                    else "직원용 앱이 종료되며 연결 서버가 중지됩니다.",
                    color = Color(0xFFB8C1C9)
                )
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF11161A)), border = BorderStroke(1.dp, Color(0xFF3A4650))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExitSummaryRow("손님용 기기", "${guestCount}대")
                        ExitSummaryRow("웹 사용자", "${webCount}명")
                        ExitSummaryRow("실행 중인 테마", "${runningRooms.size}개")
                        runningRooms.forEach { room ->
                            ExitSummaryRow("  ${room.name}", formatExitTime(room.seconds))
                        }
                    }
                }
                Text(
                    "ⓘ 손님용 타이머는 마지막으로 받은 기준 상태로 계속 진행됩니다.\n" +
                        "직원용 앱을 다시 실행하면 저장된 상태를 복원하고 다시 연결합니다.",
                    color = Color(0xFFB8C1C9),
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onConfirm, border = BorderStroke(1.dp, Color(0xFFFF5252))) {
                Text(if (hasRunningRooms) "그래도 종료" else "앱 종료", color = Color(0xFFFF5252))
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
private fun ExitSummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFD8DEE4), fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatExitTime(seconds: Int): String = "%02d:%02d".format(seconds.coerceAtLeast(0) / 60, seconds.coerceAtLeast(0) % 60)

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
    isEditing: Boolean,
    onNameChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171C20))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(if (isEditing) "프리셋 수정" else "새 프리셋 추가", color = Color(0xFFBF91EA), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = presetName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("테마 이름") }
            )
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
                Button(onClick = onSave, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DCE))) {
                    Text(if (isEditing) "수정 저장" else "프리셋 추가")
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(if (isEditing) "취소" else "닫기")
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: ThemePreset,
    ordering: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF343C44)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171C20))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(preset.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("기본 ${preset.defaultMinutes}분", color = Color(0xFF9AA3AC), fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ordering) {
                    OutlinedButton(onClick = onMoveUp, enabled = canMoveUp) { Text("↑ 위") }
                    OutlinedButton(onClick = onMoveDown, enabled = canMoveDown) { Text("↓ 아래") }
                } else {
                    OutlinedButton(onClick = onEdit) { Text("수정") }
                    TextButton(onClick = onDelete) { Text("삭제", color = Color(0xFFFF6B6B)) }
                }
            }
        }
    }
}

@Composable
private fun RoomSummaryCard(
    room: RoomInfo,
    ordering: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val statusColor = when {
        room.isMaintenance -> Color(0xFF69B8F2)
        room.isEnabled -> Color(0xFF74C98C)
        else -> Color(0xFF8D969F)
    }
    val statusText = when {
        room.isMaintenance -> "유지보수 · 손님 화면에서 숨김"
        room.isEnabled -> "사용 중 · 기본 ${room.defaultMinutes}분"
        else -> "사용 안 함 · 기본 ${room.defaultMinutes}분"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !ordering, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF3A4650)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151B20))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(room.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(statusText, color = statusColor, fontSize = 13.sp, maxLines = 1)
            }
            if (ordering) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onMoveUp, enabled = canMoveUp) { Text("↑ 위") }
                    OutlinedButton(onClick = onMoveDown, enabled = canMoveDown) { Text("↓ 아래") }
                }
            } else {
                Switch(
                    checked = room.isEnabled,
                    onCheckedChange = onEnabledChange,
                    enabled = !room.isRunning,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF07130B),
                        checkedTrackColor = statusColor,
                        uncheckedThumbColor = Color(0xFFB1B7BD),
                        uncheckedTrackColor = Color(0xFF454B51)
                    )
                )
                Text("›", color = Color.White, fontSize = 30.sp, modifier = Modifier.padding(start = 8.dp))
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
                    Text(room.id.uppercase(), color = Color(0xFF74C98C), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(if (room.isEnabled) "사용 중" else "사용 안 함", color = if (room.isEnabled) Color(0xFF44D17A) else Color(0xFF8D96A0), fontSize = 12.sp)
                }
                Switch(
                    checked = room.isEnabled,
                    onCheckedChange = onEnabledChange,
                    enabled = !room.isRunning,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF07130B),
                        checkedTrackColor = Color(0xFF74C98C)
                    )
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("유지보수 모드", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("손님용 테마 선택에서 숨기고 타이머 시작을 막습니다.", color = Color(0xFF9AA3AC), fontSize = 12.sp)
                }
                Switch(
                    checked = room.isMaintenance,
                    onCheckedChange = onMaintenanceChange,
                    enabled = room.isEnabled && !room.isRunning,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF07130B),
                        checkedTrackColor = Color(0xFF69B8F2)
                    )
                )
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onChoosePreset, enabled = hasPresets, modifier = Modifier.fillMaxWidth()) {
                Text(if (hasPresets) "테마 프리셋 적용" else "먼저 프리셋을 추가하세요")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = nameValue, onValueChange = onNameChange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("테마 이름") })
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF74C98C),
                    contentColor = Color(0xFF07130B)
                )
            ) { Text("저장", color = Color(0xFF07130B), fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                enabled = !room.isRunning,
                border = BorderStroke(1.dp, Color(0x66FF6B6B))
            ) {
                Text("테마 삭제", color = Color(0xFFFF6B6B))
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
