package com.zy.player.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.components.CinemaSectionHeader
import com.zy.player.ui.components.CinemaTopBar
import com.zy.player.ui.theme.AppColors

@Composable
fun SettingsScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSiteManagement: () -> Unit,
    onNavigateToLiveSourceManagement: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showMoreDialog by remember { mutableStateOf(false) }
    val maintenanceMessage by viewModel.maintenanceMessage.collectAsState()

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            item {
                CinemaTopBar(
                    eyebrow = "Control Room",
                    title = "设置",
                    actionIcon = Icons.Default.MoreVert,
                    actionDescription = "更多",
                    onActionClick = { showMoreDialog = true }
                )
            }

            item {
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Default.History,
                        title = "播放历史",
                        subtitle = "继续观看、进度同步、最近播放",
                        onClick = onNavigateToHistory
                    )
                    SettingsItem(
                        icon = Icons.Default.VideoLibrary,
                        title = "视频源管理",
                        subtitle = "资源站排序、检测、启用状态",
                        onClick = onNavigateToSiteManagement
                    )
                    SettingsItem(
                        icon = Icons.Default.LiveTv,
                        title = "直播源管理",
                        subtitle = "M3U 源、频道解析、线路刷新",
                        onClick = onNavigateToLiveSourceManagement,
                        showDivider = false
                    )
                }
            }

            item {
                CinemaSectionHeader(title = "应用维护", meta = "高级")
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "重置应用",
                        subtitle = "清除缓存、恢复默认数据源",
                        onClick = { showResetDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Report,
                        title = "免责声明",
                        subtitle = "内容来源、播放责任、版权说明",
                        onClick = { showDisclaimerDialog = true },
                        showDivider = false
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("重置应用") },
            text = { Text("确定要清空播放历史，并恢复默认视频源和直播源吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetApp()
                    showResetDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showMoreDialog) {
        AlertDialog(
            onDismissRequest = { showMoreDialog = false },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("更多") },
            text = { Text("ZYPlayer 当前版本支持聚合搜索、资源站换源、直播源管理、播放历史和应用重置。") },
            confirmButton = {
                TextButton(onClick = { showMoreDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }

    maintenanceMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::consumeMaintenanceMessage,
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("应用维护") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeMaintenanceMessage) {
                    Text("知道了")
                }
            }
        )
    }

    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("免责声明") },
            text = {
                Text(
                    "本应用仅供学习交流使用，所有视频内容均来自互联网，" +
                        "本应用不存储任何视频内容，不对内容的合法性、准确性、完整性负责。" +
                        "请用户自行甄别内容的真实性和合法性，如有侵权请联系删除。"
                )
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(22.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(43.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    color = AppColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = AppColors.TextTertiary,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 70.dp),
                color = AppColors.Divider
            )
        }
    }
}
