package com.zy.player.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zy.player.ui.theme.AppColors

private val CinemaShape = RoundedCornerShape(22.dp)

@Composable
fun CinemaBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B1111),
                        AppColors.Background,
                        Color(0xFF10100D)
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun CinemaTopBar(
    eyebrow: String,
    title: String,
    actionIcon: ImageVector,
    actionDescription: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = eyebrow.uppercase(),
                color = AppColors.Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp
            )
            Text(
                text = title,
                color = AppColors.TextPrimary,
                fontSize = 31.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif
            )
        }

        CinemaIconButton(
            icon = actionIcon,
            contentDescription = actionDescription,
            onClick = onActionClick
        )
    }
}

@Composable
fun CinemaIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(16.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = AppColors.TextPrimary,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
fun CinemaSectionHeader(
    title: String,
    meta: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            color = AppColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = meta,
            color = AppColors.Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CinemaSearchPill(
    text: String,
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = 18.dp,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = AppColors.TextSecondary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CinemaGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(CinemaShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = Color.White.copy(alpha = 0.045f),
        shape = CinemaShape,
        border = BorderStroke(1.dp, AppColors.Divider)
    ) {
        content()
    }
}

@Composable
fun CinemaMiniPlayButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(AppColors.Cream)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "播放",
            tint = AppColors.Background,
            modifier = Modifier.size(27.dp)
        )
    }
}

@Composable
fun CinemaLoading(
    modifier: Modifier = Modifier,
    message: String = "正在连接片库"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(AppColors.PrimaryLight)
                .border(2.dp, AppColors.Primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(AppColors.Primary)
            )
        }
        Text(
            text = message,
            color = AppColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CinemaMessage(
    title: String,
    message: String?,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    CinemaGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 34.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = AppColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = onAction,
                    color = AppColors.Cream,
                    contentColor = AppColors.Background,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = actionText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
