package com.example.escaperoomtimer.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.ui.common.ManagerStatusColors
import com.example.escaperoomtimer.ui.theme.AppText
import com.example.escaperoomtimer.ui.theme.AppTextSecondary
import com.example.escaperoomtimer.util.formatTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class DashboardRoomState(val label: String) {
    WAITING("대기"),
    RUNNING("진행 중"),
    PAUSED("일시정지"),
    FINISHED("종료"),
    MAINTENANCE("유지보수")
}

@Composable
fun RoomCard(
    room: RoomInfo,
    onTimerClick: () -> Unit,
    onResetClick: () -> Unit,
    onActionClick: () -> Unit
) {
    val state = room.dashboardState()
    val stateColor = state.color()
    val cardColor = if (state == DashboardRoomState.RUNNING) Color(0xFF1A1E21) else Color(0xFF111416)

    Surface(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(stateColor))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(36.dp).padding(start = 10.dp, end = 8.dp, top = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.name,
                        color = AppText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = state.label,
                        color = stateColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .background(stateColor.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 5.dp, vertical = 4.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(onClick = onTimerClick)
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "${room.name} 상세 화면" },
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (room.isMaintenance) "—" else formatTime(room.seconds),
                        color = room.gridTimerColor(),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = if (room.isMaintenance) "타이머 잠김" else room.expectedEndDescription(),
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                HorizontalDivider(color = Color(0xFF30363B))
                if (room.isMaintenance) {
                    MaintenanceLockControl(
                        color = ManagerStatusColors.Maintenance,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        RoomControl(
                            label = "초기화",
                            type = RoomControlType.RESET,
                            backgroundColor = Color.Transparent,
                            contentColor = AppTextSecondary,
                            onClick = onResetClick,
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF30363B)))
                        if (state == DashboardRoomState.FINISHED) {
                            Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                        } else {
                            RoomControl(
                                label = state.actionLabel(),
                                type = state.controlType(),
                                backgroundColor = state.actionColor(),
                                contentColor = state.actionContentColor(),
                                onClick = onActionClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class RoomControlType { RESET, PLAY, PAUSE }

@Composable
private fun RoomControl(
    label: String,
    type: RoomControlType,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (type) {
                RoomControlType.RESET -> "↺"
                RoomControlType.PLAY -> "▶"
                RoomControlType.PAUSE -> "Ⅱ"
            },
            color = contentColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun MaintenanceLockControl(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.semantics { contentDescription = "타이머 잠김" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(size.width * .25f, size.height * .05f),
                size = Size(size.width * .50f, size.height * .60f),
                style = stroke
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width * .16f, size.height * .43f),
                size = Size(size.width * .68f, size.height * .48f),
                cornerRadius = CornerRadius(2.dp.toPx()),
                style = stroke
            )
        }
    }
}

private fun DashboardRoomState.actionLabel(): String = when (this) {
    DashboardRoomState.WAITING -> "시작"
    DashboardRoomState.RUNNING -> "일시정지"
    DashboardRoomState.PAUSED -> "재개"
    DashboardRoomState.FINISHED -> "비활성"
    DashboardRoomState.MAINTENANCE -> "타이머 잠김"
}

private fun DashboardRoomState.controlType(): RoomControlType = when (this) {
    DashboardRoomState.WAITING,
    DashboardRoomState.PAUSED -> RoomControlType.PLAY
    DashboardRoomState.RUNNING -> RoomControlType.PAUSE
    DashboardRoomState.FINISHED,
    DashboardRoomState.MAINTENANCE -> RoomControlType.RESET
}

private fun RoomInfo.dashboardState(): DashboardRoomState = when {
    isMaintenance -> DashboardRoomState.MAINTENANCE
    status == RoomStatus.FINISHED || seconds <= 0 -> DashboardRoomState.FINISHED
    isRunning -> DashboardRoomState.RUNNING
    status == RoomStatus.PAUSED || status == RoomStatus.WARNING -> DashboardRoomState.PAUSED
    else -> DashboardRoomState.WAITING
}

private fun DashboardRoomState.color(): Color = when (this) {
    DashboardRoomState.WAITING -> ManagerStatusColors.Waiting
    DashboardRoomState.RUNNING -> ManagerStatusColors.Running
    DashboardRoomState.PAUSED -> ManagerStatusColors.Paused
    DashboardRoomState.FINISHED -> ManagerStatusColors.Finished
    DashboardRoomState.MAINTENANCE -> ManagerStatusColors.Maintenance
}

private fun DashboardRoomState.actionColor(): Color = when (this) {
    DashboardRoomState.WAITING -> ManagerStatusColors.Waiting
    DashboardRoomState.RUNNING -> ManagerStatusColors.Paused
    DashboardRoomState.PAUSED -> ManagerStatusColors.Running
    DashboardRoomState.FINISHED,
    DashboardRoomState.MAINTENANCE -> Color.Transparent
}

private fun DashboardRoomState.actionContentColor(): Color = when (this) {
    DashboardRoomState.RUNNING -> Color(0xFF111416)
    else -> Color.White
}

private fun RoomInfo.gridTimerColor(): Color = when {
    isMaintenance -> ManagerStatusColors.Maintenance
    status == RoomStatus.FINISHED || seconds <= 0 -> Color(0xFFFF4B4B)
    seconds <= 5 * 60 -> Color(0xFFFF4B4B)
    seconds <= 10 * 60 -> Color(0xFFFFA726)
    else -> AppText
}

private fun RoomInfo.expectedEndDescription(): String = if (isRunning && seconds > 0) {
    "종료 예정 ${formatClock(System.currentTimeMillis() + seconds * 1_000L)}"
} else {
    "종료 예정 --:--"
}

private fun formatClock(epochMillis: Long): String = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(epochMillis))
