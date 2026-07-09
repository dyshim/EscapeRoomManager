package com.example.escaperoomtimer.ui.home

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.util.nowText
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    rooms: List<RoomInfo>,
    onRoomClick: (RoomInfo) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(nowText()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = nowText()
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
            Text("☰", color = Color.White, fontSize = 28.sp)
            Text("방탈출 운영", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "⚙",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.clickable { onSettingsClick() }
            )
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFF2A2F35))
        Spacer(Modifier.height(14.dp))

        Text(
            text = "현재 시간",
            color = Color(0xFF8D96A0),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = currentTime,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(18.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(rooms, key = { it.id }) { room ->
                RoomCard(
                    room = room,
                    onClick = { onRoomClick(room) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                Toast.makeText(context, "새 방 추가는 다음 버전에서 넣을게요.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00))
        ) {
            Text("+ 새 방 추가", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
