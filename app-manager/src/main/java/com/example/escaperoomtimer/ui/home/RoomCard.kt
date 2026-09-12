package com.example.escaperoomtimer.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.ui.common.ManagerStatusColors
import com.example.escaperoomtimer.ui.theme.AppText
import com.example.escaperoomtimer.ui.theme.AppTextSecondary
import com.example.escaperoomtimer.util.formatRemainingTime

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
    connectedDeviceCount: Int,
    onTimerClick: () -> Unit,
    onAdjustClick: () -> Unit,
    onResetClick: () -> Unit,
    onActionClick: () -> Unit
) {
    val state = room.dashboardState()
    val stateColor = state.color()
    val cardColor = if (state == DashboardRoomState.RUNNING) Color(0xFF1A1E21) else Color(0xFF111416)

    Surface(
        modifier = Modifier.fillMaxWidth().height(164.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(stateColor))
            Column(modifier = Modifier.weight(1f)) {
                ThemeName(
                    name = room.name,
                    modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(onClick = onTimerClick)
                        .semantics { contentDescription = "${room.name} 상세 화면" },
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (room.isMaintenance) "—" else formatRemainingTime(room.seconds),
                        color = room.gridTimerColor(),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                    if (room.isMaintenance) {
                        Text(
                            text = "타이머 잠김",
                            color = AppTextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
                RoomStatusRow(
                    label = state.label,
                    stateColor = stateColor,
                    connectedDeviceCount = connectedDeviceCount,
                    modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 10.dp)
                )
                HorizontalDivider(color = Color(0xFF30363B))
                if (room.isMaintenance) {
                    MaintenanceLockControl(
                        color = ManagerStatusColors.Maintenance,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoomControl(
                            label = "시간 조정",
                            symbol = "±",
                            backgroundColor = Color.Transparent,
                            contentColor = AppTextSecondary,
                            enabled = state != DashboardRoomState.FINISHED,
                            onClick = onAdjustClick,
                            modifier = Modifier.weight(1f)
                        )
                        RoomControl(
                            label = state.actionLabel(),
                            symbol = if (state == DashboardRoomState.RUNNING) "Ⅱ" else "▶",
                            backgroundColor = state.actionColor(),
                            contentColor = state.actionContentColor(),
                            enabled = state != DashboardRoomState.FINISHED,
                            onClick = onActionClick,
                            modifier = Modifier.weight(1f)
                        )
                        RoomControl(
                            label = "초기화",
                            symbol = "↺",
                            backgroundColor = Color.Transparent,
                            contentColor = AppTextSecondary,
                            enabled = state != DashboardRoomState.WAITING,
                            onClick = onResetClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomControl(
    label: String,
    symbol: String,
    backgroundColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (enabled) backgroundColor else Color.Transparent,
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    color = if (enabled) contentColor else contentColor.copy(alpha = 0.32f),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ThemeName(name: String, modifier: Modifier = Modifier) {
    val fontSizes = listOf(18.sp, 17.sp, 16.sp)
    var fontSizeIndex by remember(name) { mutableIntStateOf(0) }
    val fontSize = fontSizes[fontSizeIndex]

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = name,
            color = AppText,
            fontSize = fontSize,
            lineHeight = (fontSize.value + 2).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if ((result.didOverflowWidth || result.didOverflowHeight) && fontSizeIndex < fontSizes.lastIndex) {
                    fontSizeIndex += 1
                }
            }
        )
    }
}

@Composable
private fun RoomStatusRow(
    label: String,
    stateColor: Color,
    connectedDeviceCount: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Text(
            text = label,
            color = stateColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.align(Alignment.Center),
        )
        if (connectedDeviceCount > 0) {
            Text(
                text = "연결 $connectedDeviceCount",
                color = AppTextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
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
