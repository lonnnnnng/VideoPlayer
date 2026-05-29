package com.zy.player.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BrightCinemaColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.OnPrimary,
    secondary = AppColors.Accent,
    onSecondary = AppColors.OnPrimary,
    background = AppColors.Background,
    surface = AppColors.Surface,
    surfaceVariant = AppColors.SurfaceAlt,
    onSurface = AppColors.TextPrimary,
    onBackground = AppColors.TextPrimary,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.Divider,
    error = AppColors.Error,
    onError = AppColors.OnPrimary
)

private val StableCinemaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(6.dp)
)

@Composable
fun ZYPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BrightCinemaColorScheme,
        shapes = StableCinemaShapes,
        content = content
    )
}
