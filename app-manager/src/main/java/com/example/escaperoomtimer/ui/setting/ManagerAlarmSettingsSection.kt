package com.example.escaperoomtimer.ui.setting

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.alarm.ManagerGameEndAlarmController
import com.example.escaperoomtimer.settings.ManagerAlarmPreferences
import com.example.escaperoomtimer.settings.ManagerAlarmSettings

private val stopOptions = listOf(5, 10, 15, 20, 30, 45, 60, 0)

@Composable
fun ManagerAlarmSettingsSection() {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(ManagerAlarmPreferences.load(context)) }
    var showStopMenu by remember { mutableStateOf(false) }

    fun save(updated: ManagerAlarmSettings) {
        settings = updated
        ManagerAlarmPreferences.save(context, updated)
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            save(settings.copy(soundUri = uri?.toString()))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171C20))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "직원용 종료 알람",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "알람음, 자동 정지 시간, 진동을 이 기기에서 따로 설정합니다.",
                color = Color(0xFF8D96A0),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(14.dp))

            Text("알람음", color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                text = ringtoneTitle(settings.soundUri),
                color = Color(0xFFFFB000),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    Text("알람음 선택")
                }
                Button(
                    onClick = {
                        ManagerGameEndAlarmController.preview(
                            context,
                            settings.soundUri,
                            settings.volumePercent
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("미리듣기")
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("알람 음량", color = Color.White, fontWeight = FontWeight.Bold)
                Text("${settings.volumePercent}%", color = Color(0xFFFFB000), fontSize = 14.sp)
            }
            Slider(
                value = settings.volumePercent.toFloat(),
                onValueChange = { value ->
                    settings = settings.copy(volumePercent = value.toInt().coerceIn(0, 100))
                },
                onValueChangeFinished = { ManagerAlarmPreferences.save(context, settings) },
                valueRange = 0f..100f
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("자동 정지", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = autoStopLabel(settings.autoStopSeconds),
                        color = Color(0xFF8D96A0),
                        fontSize = 13.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    OutlinedButton(onClick = { showStopMenu = true }) {
                        Text(autoStopLabel(settings.autoStopSeconds))
                    }
                    DropdownMenu(
                        expanded = showStopMenu,
                        onDismissRequest = { showStopMenu = false }
                    ) {
                        stopOptions.forEach { seconds ->
                            DropdownMenuItem(
                                text = { Text(autoStopLabel(seconds)) },
                                onClick = {
                                    save(settings.copy(autoStopSeconds = seconds))
                                    showStopMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("진동", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (settings.vibrationEnabled) "사용" else "사용 안 함",
                        color = Color(0xFF8D96A0),
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { save(settings.copy(vibrationEnabled = it)) }
                )
            }
        }
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
