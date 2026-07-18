package com.example.escaperoomtimer.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.util.formatTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WaitingPurple = Color(0xFFB968F4)
private val RunningGreen = Color(0xFF31D77B)
private val ConnectionCyan = Color(0xFF00D7D0)
private val PausedYellow = Color(0xFFFFD600)
private val FinishedRed = Color(0xFFFF3B3B)
private val MaintenanceBlue = Color(0xFF2196F3)

private enum class DashboardRoomState { WAITING, RUNNING, PAUSED, FINISHED, MAINTENANCE }

@Composable
fun RoomCard(
    room: RoomInfo,
    connectedDisplays: Int,
    onClick: () -> Unit
) {
    val state = room.dashboardState()
    val stateColor = when (state) {
        DashboardRoomState.WAITING -> WaitingPurple
        DashboardRoomState.RUNNING -> RunningGreen
        DashboardRoomState.PAUSED -> PausedYellow
        DashboardRoomState.FINISHED -> FinishedRed
        DashboardRoomState.MAINTENANCE -> MaintenanceBlue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoomStateIcon(state = state, color = stateColor)
        Column(modifier = Modifier.padding(start = 13.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(room.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (connectedDisplays > 0) {
                    GuestDeviceIcon(
                        color = ConnectionCyan,
                        modifier = Modifier.padding(start = 10.dp).size(13.dp)
                    )
                    Text(
                        "$connectedDisplays",
                        color = ConnectionCyan,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 3.dp)
                    )
                }
            }
            Text(
                room.statusDescription(state),
                color = stateColor,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        Text(
            if (state == DashboardRoomState.MAINTENANCE) "—" else formatTime(room.seconds),
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Text("›", color = Color.White, fontSize = 30.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun RoomInfo.dashboardState(): DashboardRoomState = when {
    isMaintenance -> DashboardRoomState.MAINTENANCE
    status == RoomStatus.FINISHED || seconds <= 0 -> DashboardRoomState.FINISHED
    isRunning -> DashboardRoomState.RUNNING
    status == RoomStatus.PAUSED || status == RoomStatus.WARNING -> DashboardRoomState.PAUSED
    else -> DashboardRoomState.WAITING
}

private fun RoomInfo.statusDescription(state: DashboardRoomState): String = when (state) {
    DashboardRoomState.WAITING -> "대기 · 기본 ${defaultMinutes}분"
    DashboardRoomState.RUNNING -> "진행 중 · ${timestampLabel("시작", startedAtEpochMillis)}"
    DashboardRoomState.PAUSED -> "일시정지 · 소요 ${formatTime(elapsedSeconds)}"
    DashboardRoomState.FINISHED -> "종료 · ${timestampLabel("실제 종료", finishedAtEpochMillis)}"
    DashboardRoomState.MAINTENANCE -> "유지보수 · 손님용 선택에서 숨김"
}

@Composable
private fun RoomStateIcon(state: DashboardRoomState, color: Color) {
    Canvas(modifier = Modifier.size(42.dp)) {
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(color, style = stroke)
        when (state) {
            DashboardRoomState.WAITING -> {
                drawLine(color, Offset(size.width * .35f, size.height * .27f), Offset(size.width * .65f, size.height * .27f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * .35f, size.height * .73f), Offset(size.width * .65f, size.height * .73f), stroke.width, StrokeCap.Round)
                val hourglass = Path().apply {
                    moveTo(size.width * .38f, size.height * .30f)
                    cubicTo(size.width * .39f, size.height * .43f, size.width * .46f, size.height * .46f, size.width * .50f, size.height * .50f)
                    cubicTo(size.width * .54f, size.height * .54f, size.width * .61f, size.height * .57f, size.width * .62f, size.height * .70f)
                    moveTo(size.width * .62f, size.height * .30f)
                    cubicTo(size.width * .61f, size.height * .43f, size.width * .54f, size.height * .46f, size.width * .50f, size.height * .50f)
                    cubicTo(size.width * .46f, size.height * .54f, size.width * .39f, size.height * .57f, size.width * .38f, size.height * .70f)
                }
                drawPath(hourglass, color, style = stroke)
            }
            DashboardRoomState.RUNNING -> {
                val play = Path().apply {
                    moveTo(size.width * .40f, size.height * .32f)
                    lineTo(size.width * .68f, size.height * .50f)
                    lineTo(size.width * .40f, size.height * .68f)
                    close()
                }
                drawPath(play, color, style = stroke)
            }
            DashboardRoomState.PAUSED -> {
                drawLine(color, Offset(size.width * .43f, size.height * .34f), Offset(size.width * .43f, size.height * .66f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * .57f, size.height * .34f), Offset(size.width * .57f, size.height * .66f), stroke.width, StrokeCap.Round)
            }
            DashboardRoomState.FINISHED -> {
                drawLine(color, Offset(size.width * .32f, size.height * .51f), Offset(size.width * .45f, size.height * .64f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * .45f, size.height * .64f), Offset(size.width * .70f, size.height * .37f), stroke.width, StrokeCap.Round)
            }
            DashboardRoomState.MAINTENANCE -> {
                drawLine(color, Offset(size.width * .35f, size.height * .68f), Offset(size.width * .66f, size.height * .36f), stroke.width, StrokeCap.Round)
                drawCircle(color, radius = size.width * .07f, center = Offset(size.width * .35f, size.height * .68f), style = stroke)
                drawArc(color, 20f, 210f, false, Offset(size.width * .50f, size.height * .25f), Size(size.width * .20f, size.height * .20f), style = stroke)
            }
        }
    }
}

@Composable
private fun GuestDeviceIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.4.dp.toPx())
        drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = stroke)
        drawCircle(color, radius = 0.8.dp.toPx(), center = Offset(size.width / 2f, size.height * .86f))
    }
}

private fun timestampLabel(label: String, epochMillis: Long?): String =
    epochMillis?.let { "$label ${formatClock(it)}" } ?: "$label --:--"

private fun formatClock(epochMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(epochMillis))
