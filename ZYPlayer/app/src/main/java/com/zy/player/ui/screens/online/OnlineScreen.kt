package com.zy.player.ui.screens.online

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zy.player.domain.model.LiveChannel
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.theme.AppColors

private enum class OnlineLinkMode {
    M3u8,
    M3u
}

@Composable
fun OnlineScreen(
    prefillUrl: String? = null,
    onNavigateToM3u8Player: (String) -> Unit,
    onNavigateToLivePlayer: (LiveChannel) -> Unit,
    viewModel: OnlineViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    var inputUrl by remember { mutableStateOf("") }
    val parseUiState by viewModel.parseUiState.collectAsState()
    val validation = remember(inputUrl) { validateOnlineLink(inputUrl) }
    val mode = remember(inputUrl) { inferOnlineLinkMode(inputUrl) }

    LaunchedEffect(prefillUrl) {
        val normalizedUrl = prefillUrl?.trim().orEmpty()
        if (normalizedUrl.isNotBlank() && normalizedUrl != inputUrl) {
            inputUrl = normalizedUrl
            viewModel.clearParseResult()
        }
    }

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = 18.dp,
                end = 18.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OnlineIntroCopy()
            }

            item {
                OnlineInputField(
                    inputUrl = inputUrl,
                    onInputChange = {
                        inputUrl = it
                        viewModel.clearParseResult()
                    }
                )
            }

            item {
                OnlineActionButton(
                    enabled = validation.isPlayable,
                    isLoading = parseUiState.isLoading,
                    onActionClick = {
                        focusManager.clearFocus(force = true)
                        val playableUrl = inputUrl.trim()
                        when (mode) {
                            OnlineLinkMode.M3u8 -> onNavigateToM3u8Player(playableUrl)
                            OnlineLinkMode.M3u -> viewModel.parseM3u(playableUrl)
                        }
                    }
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
private fun OnlineIntroCopy() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "在线播放",
            color = AppColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "粘贴 m3u8 视频流或 m3u 频道列表链接，系统会自动识别并进入对应播放器。",
            color = AppColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun OnlineInputField(
    inputUrl: String,
    onInputChange: (String) -> Unit
) {
    OutlinedTextField(
        value = inputUrl,
        onValueChange = onInputChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        label = { Text("播放链接") },
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
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppColors.TextPrimary,
            unfocusedTextColor = AppColors.TextPrimary,
            focusedContainerColor = Color.White.copy(alpha = 0.045f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.045f),
            focusedBorderColor = AppColors.Primary,
            unfocusedBorderColor = AppColors.Divider,
            cursorColor = AppColors.Primary,
            focusedLabelColor = AppColors.Primary,
            unfocusedLabelColor = AppColors.TextSecondary,
            focusedPlaceholderColor = AppColors.TextTertiary,
            unfocusedPlaceholderColor = AppColors.TextTertiary
        )
    )
}

@Composable
private fun OnlineActionButton(
    enabled: Boolean,
    isLoading: Boolean,
    onActionClick: () -> Unit
) {
    Surface(
        onClick = onActionClick,
        enabled = enabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
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
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isLoading) "解析中" else "解析播放",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.Surface.copy(alpha = 0.78f))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.message != null) {
            Text(
                text = state.message,
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
            .height(54.dp),
        color = Color.White.copy(alpha = 0.045f),
        contentColor = AppColors.TextPrimary,
        shape = RoundedCornerShape(14.dp),
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

private data class OnlineLinkValidation(
    val isPlayable: Boolean
)

private fun inferOnlineLinkMode(input: String): OnlineLinkMode {
    val normalized = input.trim().lowercase()
    return if (normalized.endsWith(".m3u") || normalized.contains(".m3u?")) {
        OnlineLinkMode.M3u
    } else {
        OnlineLinkMode.M3u8
    }
}

private fun validateOnlineLink(input: String): OnlineLinkValidation {
    val trimmed = input.trim()
    val lower = trimmed.lowercase()
    val hasScheme = lower.startsWith("http://") || lower.startsWith("https://")
    val hasSupportedPlaylist = lower.contains(".m3u8") || lower.contains(".m3u")
    return OnlineLinkValidation(
        isPlayable = trimmed.isNotBlank() && hasScheme && hasSupportedPlaylist
    )
}
