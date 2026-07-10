package com.example.escaperoomtimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AppOrange,
    onPrimary = AppBlack,
    secondary = AppGreen,
    tertiary = AppPurple,
    background = AppBlack,
    onBackground = AppText,
    surface = AppSurface,
    onSurface = AppText,
    surfaceVariant = AppSurfaceHigh,
    onSurfaceVariant = AppTextSecondary,
    error = AppRed,
    onError = AppText
)

@Composable
fun EscapeRoomTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
