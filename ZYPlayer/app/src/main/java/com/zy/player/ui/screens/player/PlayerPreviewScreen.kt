package com.zy.player.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.components.CinemaTopBar
import com.zy.player.ui.components.NetworkImage
import com.zy.player.ui.theme.AppColors

private const val PreviewBackdropUrl =
    "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=900&q=80"

@Composable
fun PlayerPreviewScreen() {
    var showCastDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            item {
                CinemaTopBar(
                    eyebrow = "Now Playing",
                    title = "沉浸播放",
                    actionIcon = Icons.Default.Cast,
                    actionDescription = "投屏",
                    onActionClick = { showCastDialog = true }
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth()
                        .height(292.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(AppColors.Surface)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(25.dp))
                ) {
                    NetworkImage(
                        url = PreviewBackdropUrl,
                        contentDescription = "沉浸播放",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        AppColors.Background.copy(alpha = 0.06f),
                                        AppColors.Background.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )

                    Text(
                        text = "正在播放",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(18.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(AppColors.Primary)
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        color = AppColors.Background,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )

                    Surface(
                        onClick = { showPreviewDialog = true },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(74.dp),
                        color = Color.Black.copy(alpha = 0.54f),
                        contentColor = AppColors.TextPrimary,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "播放",
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "暗线追踪 · 第 12 集",
                        color = AppColors.TextPrimary,
                        fontSize = 29.sp,
                        lineHeight = 33.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "上次看到 46%，自动记录播放进度。线路来自量子资源，支持倍速和后台画中画。",
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    LinearProgressIndicator(
                        progress = { 0.46f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = AppColors.Primary,
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(8) { index ->
                        val label = if (index == 4) "12" else (index + 1).toString().padStart(2, '0')
                        val active = index == 4
                        Surface(
                            color = if (active) AppColors.Accent else Color.White.copy(alpha = 0.04f),
                            contentColor = if (active) AppColors.Background else AppColors.TextSecondary,
                            shape = RoundedCornerShape(14.dp),
                            border = if (active) null else androidx.compose.foundation.BorderStroke(1.dp, AppColors.Divider)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCastDialog) {
        AlertDialog(
            onDismissRequest = { showCastDialog = false },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("投屏") },
            text = { Text("当前未发现可用投屏设备，请确认电视或盒子与手机在同一网络。") },
            confirmButton = {
                TextButton(onClick = { showCastDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("播放预览") },
            text = { Text("请从首页或搜索结果进入影片详情，再选择具体剧集开始播放。") },
            confirmButton = {
                TextButton(onClick = { showPreviewDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }
}
