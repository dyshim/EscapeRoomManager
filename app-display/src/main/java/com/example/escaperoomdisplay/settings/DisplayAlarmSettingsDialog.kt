package com.example.escaperoomdisplay.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.escaperoomdisplay.alarm.DisplayGameEndAlarmController

private val stopOptions = listOf(5, 10, 15, 20, 30, 45, 60, 0)

@Composable
fun DisplayAlarmSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(DisplayAlarmPreferences.load(context)) }
    var showStopDialog by remember { mutableStateOf(false) }

    fun save(updated: DisplayAlarmSettings) {
        settings = updated
        DisplayAlarmPreferences.save(context, updated)
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            save(settings.copy(soundUri = uri?.toString()))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("손님용 종료 알람") },
        text = {
            Column {
                Text("이 기기에서 사용할 알람 설정입니다.")
                Spacer(Modifier.height(14.dp))

                Text("알람음", fontWeight = FontWeight.Bold)
                Text(ringtoneTitle(settings.soundUri))
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val currentUri = settings.soundUri?.let(Uri::parse)
                                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                            }
                            ringtoneLauncher.launch(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("선택")
                    }
                    Button(
                        onClick = {
                            DisplayGameEndAlarmController.preview(context, settings.soundUri)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("미리듣기")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("자동 정지", fontWeight = FontWeight.Bold)
                        Text(autoStopLabel(settings.autoStopSeconds))
                    }
                    OutlinedButton(onClick = { showStopDialog = true }) {
                        Text("변경")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("진동", fontWeight = FontWeight.Bold)
                        Text(if (settings.vibrationEnabled) "사용" else "사용 안 함")
                    }
                    Switch(
                        checked = settings.vibrationEnabled,
                        onCheckedChange = { save(settings.copy(vibrationEnabled = it)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("완료") }
        }
    )

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("자동 정지 시간") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    stopOptions.forEach { seconds ->
                        TextButton(
                            onClick = {
                                save(settings.copy(autoStopSeconds = seconds))
                                showStopDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(autoStopLabel(seconds))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("닫기") }
            }
        )
    }
}

@Composable
private fun ringtoneTitle(savedUri: String?): String {
    val context = LocalContext.current
    val uri = savedUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    return runCatching {
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
    }.getOrNull() ?: "기본 알람음"
}

private fun autoStopLabel(seconds: Int): String {
    return if (seconds <= 0) "자동 정지 안 함" else "${seconds}초 후 정지"
}
