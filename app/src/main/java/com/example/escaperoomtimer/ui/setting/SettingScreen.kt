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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

@Composable
fun SettingScreen(
    rooms: List<RoomInfo>,
    onBack: () -> Unit,
    onSaveRoom: (roomId: String, name: String, defaultMinutes: Int) -> Unit
) {
    val context = LocalContext.current
    val nameInputs = remember { mutableStateMapOf<String, String>() }
    val minuteInputs = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(rooms.size) {
        rooms.forEach { room ->
            if (nameInputs[room.id] == null) nameInputs[room.id] = room.name
            if (minuteInputs[room.id] == null) minuteInputs[room.id] = room.defaultMinutes.toString()
        }
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
            Text(
                text = "←",
                color = Color.White,
                fontSize = 28.sp,
                modifier = Modifier.clickable { onBack() }
            )
            Text("방 설정", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("저장", color = Color(0xFFFFB000), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFF2A2F35))
        Spacer(Modifier.height(14.dp))

        Text(
            text = "방 이름과 기본 플레이 시간을 설정할 수 있어요.",
            color = Color(0xFF8D96A0),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rooms, key = { it.id }) { room ->
                RoomSettingCard(
                    room = room,
                    nameValue = nameInputs[room.id] ?: room.name,
                    minutesValue = minuteInputs[room.id] ?: room.defaultMinutes.toString(),
                    onNameChange = { nameInputs[room.id] = it },
                    onMinutesChange = { value ->
                        minuteInputs[room.id] = value.filter { it.isDigit() }.take(3)
                    },
                    onSave = {
                        val minutes = minuteInputs[room.id]?.toIntOrNull() ?: room.defaultMinutes
                        onSaveRoom(room.id, nameInputs[room.id] ?: room.name, minutes)
                        Toast.makeText(context, "${nameInputs[room.id] ?: room.name} 저장 완료", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun RoomSettingCard(
    room: RoomInfo,
    nameValue: String,
    minutesValue: String,
    onNameChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171C20)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = room.id.uppercase(),
                color = Color(0xFFFFB000),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = nameValue,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("방 이름") }
            )

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00))
            ) {
                Text("저장", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
