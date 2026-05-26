package com.zy.player.ui.screens.online

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zy.player.domain.model.LiveChannel
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.components.CinemaTopBar
import com.zy.player.ui.theme.AppColors

private enum class OnlineLinkMode(
    val label: String,
    val title: String,
    val icon: ImageVector
) {
    Auto("自动", "自动识别", Icons.Default.Link),
    M3u8("M3U8", "单路播放", Icons.Default.PlayArrow),
    M3u("M3U", "列表解析", Icons.AutoMirrored.Filled.PlaylistPlay)
}

@Composable
fun OnlineScreen(
    onNavigateToM3u8Player: (String) -> Unit,
    onNavigateToLivePlayer: (LiveChannel) -> Unit,
    viewModel: OnlineViewModel = hiltViewModel()
) {
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    var inputUrl by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(OnlineLinkMode.Auto) }
    val parseUiState by viewModel.parseUiState.collectAsState()

    val inferredMode = remember(inputUrl, selectedMode) {
        if (selectedMode != OnlineLinkMode.Auto) {
            selectedMode
        } else {
            inferOnlineLinkMode(inputUrl)
        }
    }
    val validation = remember(inputUrl, inferredMode) {
        validateOnlineLink(inputUrl, inferredMode)
    }

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                CinemaTopBar(
                    eyebrow = "URL Player",
                    title = "在线播放",
                    actionIcon = Icons.Default.ContentPaste,
                    actionDescription = "粘贴链接",
                    onActionClick = {
                        clipboardManager.getText()?.text?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { pastedUrl ->
                                inputUrl = pastedUrl
                                viewModel.clearParseResult()
                            }
                    }
                )
            }

            item {
                OnlineModeTabs(
                    selectedMode = selectedMode,
                    activeMode = inferredMode,
                    onModeClick = { selectedMode = it }
                )
            }

            item {
                OnlineInputPanel(
                    inputUrl = inputUrl,
                    onInputChange = {
                        inputUrl = it
                        viewModel.clearParseResult()
                    },
                    validation = validation
                )
            }

            item {
                OnlineActionPanel(
                    mode = inferredMode,
                    enabled = validation.isPlayable,
                    isLoading = parseUiState.isLoading,
                    onActionClick = {
                        focusManager.clearFocus(force = true)
                        val playableUrl = inputUrl.trim()
                        when (inferredMode) {
                            OnlineLinkMode.M3u8 -> onNavigateToM3u8Player(playableUrl)
                            OnlineLinkMode.M3u -> viewModel.parseM3u(playableUrl)
                            OnlineLinkMode.Auto -> onNavigateToM3u8Player(playableUrl)
                        }
                    }
                )
            }

            item {
                OnlinePreviewPanel(
                    inputUrl = inputUrl,
                    mode = inferredMode,
                    validation = validation
                )
            }

            if (parseUiState.message != null || parseUiState.channels.isNotEmpty()) {
                item {
                    OnlineParseResultPanel(
                        state = parseUiState,
                        onChannelClick = onNavigateToLivePlayer
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineModeTabs(
    selectedMode: OnlineLinkMode,
    activeMode: OnlineLinkMode,
    onModeClick: (OnlineLinkMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OnlineLinkMode.values().forEach { mode ->
            val selected = selectedMode == mode
            val active = activeMode == mode
            Surface(
                onClick = { onModeClick(mode) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                color = if (selected) AppColors.Primary else Color.White.copy(alpha = 0.045f),
                contentColor = if (selected) AppColors.Background else AppColors.TextPrimary,
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(
                    1.dp,
                    if (active && !selected) AppColors.Primary.copy(alpha = 0.55f) else AppColors.Divider
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = mode.icon,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = mode.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineInputPanel(
    inputUrl: String,
    onInputChange: (String) -> Unit,
    validation: OnlineLinkValidation
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "播放链接",
                    color = AppColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = validation.status,
                    color = if (validation.isPlayable) AppColors.Primary else AppColors.TextTertiary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = { onInputChange("") },
                enabled = inputUrl.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("清空")
            }
        }

        OutlinedTextField(
            value = inputUrl,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp),
            placeholder = { Text("https://.../index.m3u8 或 https://.../playlist.m3u") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = AppColors.Primary
                )
            },
            minLines = 3,
            maxLines = 4,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppColors.TextPrimary,
                unfocusedTextColor = AppColors.TextPrimary,
                focusedContainerColor = AppColors.Background.copy(alpha = 0.52f),
                unfocusedContainerColor = AppColors.Background.copy(alpha = 0.52f),
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Divider,
                cursorColor = AppColors.Primary,
                focusedPlaceholderColor = AppColors.TextTertiary,
                unfocusedPlaceholderColor = AppColors.TextTertiary
            )
        )
    }
}

@Composable
private fun OnlinePreviewPanel(
    inputUrl: String,
    mode: OnlineLinkMode,
    validation: OnlineLinkValidation
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AppColors.Surface.copy(alpha = 0.78f))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = mode.icon,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = mode.title,
                    color = AppColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = validation.previewTitle,
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OnlineTypeBadge(mode = mode)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OnlineMetaPill(validation.scheme)
            OnlineMetaPill(validation.extension)
            OnlineMetaPill(if (mode == OnlineLinkMode.M3u) "频道列表" else "直接播放")
        }

        OnlineRoutePreview(
            inputUrl = inputUrl,
            mode = mode,
            validation = validation
        )
    }
}

@Composable
private fun OnlineRoutePreview(
    inputUrl: String,
    mode: OnlineLinkMode,
    validation: OnlineLinkValidation
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OnlineRouteRow(
            index = "1",
            title = if (mode == OnlineLinkMode.M3u) "解析 M3U 列表" else "读取媒体地址",
            subtitle = validation.previewUrl,
            active = validation.isPlayable
        )
        OnlineRouteRow(
            index = "2",
            title = if (mode == OnlineLinkMode.M3u) "选择频道" else "创建在线播放",
            subtitle = if (mode == OnlineLinkMode.M3u) "展开频道、分组、线路" else "沿用视频播放页控制区",
            active = validation.isPlayable && inputUrl.isNotBlank()
        )
        OnlineRouteRow(
            index = "3",
            title = "进入播放器",
            subtitle = if (validation.isPlayable) "播放、投屏、全屏、统计信息" else "等待有效链接",
            active = validation.isPlayable
        )
    }
}

@Composable
private fun OnlineActionPanel(
    mode: OnlineLinkMode,
    enabled: Boolean,
    isLoading: Boolean,
    onActionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            onClick = onActionClick,
            enabled = enabled && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            color = if (enabled && !isLoading) AppColors.Primary else Color.White.copy(alpha = 0.06f),
            contentColor = if (enabled && !isLoading) AppColors.Background else AppColors.TextTertiary,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (enabled && !isLoading) Color.Transparent else AppColors.Divider)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (mode == OnlineLinkMode.M3u) Icons.AutoMirrored.Filled.PlaylistPlay else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isLoading -> "解析中"
                        mode == OnlineLinkMode.M3u -> "解析频道列表"
                        else -> "解析播放"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OnlineRecentChip(title = "CCTV 直播", meta = "m3u8", modifier = Modifier.weight(1f))
            OnlineRecentChip(title = "本地列表", meta = "m3u", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun OnlineParseResultPanel(
    state: OnlineParseUiState,
    onChannelClick: (LiveChannel) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AppColors.Surface.copy(alpha = 0.78f))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "解析结果",
                color = AppColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = state.message.orEmpty(),
                color = if (state.channels.isNotEmpty()) AppColors.Primary else AppColors.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        state.channels.take(30).forEach { channel ->
            OnlineChannelRow(
                channel = channel,
                onClick = { onChannelClick(channel) }
            )
        }
    }
}

@Composable
private fun OnlineChannelRow(
    channel: LiveChannel,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        color = Color.White.copy(alpha = 0.045f),
        contentColor = AppColors.TextPrimary,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, AppColors.Divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LiveTv,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = channel.name,
                    color = AppColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${channel.group} · ${channel.format}",
                    color = AppColors.TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun OnlineRouteRow(
    index: String,
    title: String,
    subtitle: String,
    active: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (active) AppColors.PrimaryLight else Color.White.copy(alpha = 0.05f))
                .border(1.dp, if (active) AppColors.Primary.copy(alpha = 0.35f) else AppColors.Divider, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index,
                color = if (active) AppColors.Primary else AppColors.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = AppColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = AppColors.TextTertiary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OnlineTypeBadge(mode: OnlineLinkMode) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AppColors.Cream)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (mode == OnlineLinkMode.M3u) Icons.Default.LiveTv else mode.icon,
            contentDescription = null,
            tint = AppColors.Background,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = mode.label,
            color = AppColors.Background,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun OnlineMetaPill(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AppColors.SurfaceAlt)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        color = AppColors.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun OnlineRecentChip(
    title: String,
    meta: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(15.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(18.dp)
        )
        Column(
            modifier = Modifier.padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = title,
                color = AppColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta,
                color = AppColors.TextTertiary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

private data class OnlineLinkValidation(
    val isPlayable: Boolean,
    val status: String,
    val previewTitle: String,
    val previewUrl: String,
    val scheme: String,
    val extension: String
)

private fun inferOnlineLinkMode(input: String): OnlineLinkMode {
    val normalized = input.trim().lowercase()
    return when {
        normalized.endsWith(".m3u") || normalized.contains(".m3u?") -> OnlineLinkMode.M3u
        normalized.endsWith(".m3u8") || normalized.contains(".m3u8?") -> OnlineLinkMode.M3u8
        else -> OnlineLinkMode.M3u8
    }
}

private fun validateOnlineLink(input: String, mode: OnlineLinkMode): OnlineLinkValidation {
    val trimmed = input.trim()
    val lower = trimmed.lowercase()
    val hasScheme = lower.startsWith("http://") || lower.startsWith("https://")
    val matchesMode = when (mode) {
        OnlineLinkMode.M3u -> lower.contains(".m3u") && !lower.contains(".m3u8")
        OnlineLinkMode.M3u8 -> lower.contains(".m3u8")
        OnlineLinkMode.Auto -> lower.contains(".m3u")
    }
    val playable = trimmed.isNotBlank() && hasScheme && matchesMode
    val extension = when {
        lower.contains(".m3u8") -> "m3u8"
        lower.contains(".m3u") -> "m3u"
        else -> "未知"
    }
    return OnlineLinkValidation(
        isPlayable = playable,
        status = when {
            trimmed.isBlank() -> "等待输入"
            !hasScheme -> "需要 http 或 https 链接"
            matchesMode -> "链接格式可解析"
            else -> "当前模式不匹配"
        },
        previewTitle = if (playable) {
            if (mode == OnlineLinkMode.M3u) "预计解析为频道列表" else "预计进入在线播放"
        } else {
            "未生成播放预览"
        },
        previewUrl = trimmed.ifBlank { "未输入链接" },
        scheme = when {
            lower.startsWith("https://") -> "HTTPS"
            lower.startsWith("http://") -> "HTTP"
            else -> "URL"
        },
        extension = extension
    )
}
