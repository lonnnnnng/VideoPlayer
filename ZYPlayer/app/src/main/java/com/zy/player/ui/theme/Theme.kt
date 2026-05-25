package com.zy.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkCinemaColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.Background,
    secondary = AppColors.Accent,
    background = AppColors.Background,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    onBackground = AppColors.TextPrimary,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.Divider,
    error = AppColors.Error
)

@Composable
fun ZYPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkCinemaColorScheme,
        content = content
    )
}
