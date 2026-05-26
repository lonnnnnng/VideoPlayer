package com.zy.player.ui.screens.live

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zy.player.domain.model.LiveChannel
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.components.CinemaLoading
import com.zy.player.ui.components.CinemaMessage
import com.zy.player.ui.components.CinemaSectionHeader
import com.zy.player.ui.theme.AppColors

@Composable
fun LiveScreen(
    onNavigateToPlayer: (LiveChannel) -> Unit,
    viewModel: LiveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val currentSourceId by viewModel.currentSourceId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()

    var showSourceSelector by remember { mutableStateOf(false) }
    val groups = remember(uiState) { viewModel.getGroups() }
    val currentSourceName = sources.firstOrNull { it.id == currentSourceId }?.name ?: "选择直播源"

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 18.dp)
        ) {
            item {
                LiveSearchSourceRow(
                    searchQuery = searchQuery,
                    currentSourceName = currentSourceName,
                    onSearchChange = viewModel::setSearchQuery,
                    onSourceClick = { showSourceSelector = true }
                )
            }

            item {
                SourceTabs(
                    labels = groups.ifEmpty { listOf("央视", "卫视", "体育", "电影", "少儿") },
                    selected = selectedGroup,
                    onClick = { group -> viewModel.selectGroup(if (group == selectedGroup) null else group) }
                )
            }

            when (val state = uiState) {
                is LiveUiState.Loading -> item { CinemaLoading(message = "正在解析直播源") }
                is LiveUiState.Error -> item {
                    CinemaMessage(
                        title = "直播源连接失败",
                        message = state.message,
                        actionText = "重试",
                        onAction = { currentSourceId?.let { viewModel.selectSource(it) } }
                    )
                }
                is LiveUiState.Empty -> item {
                    CinemaMessage(
                        title = "暂无频道",
                        message = "当前筛选没有频道，清除搜索或切换分组再试。"
                    )
                }
                is LiveUiState.Success -> {
                    item {
                        CinemaSectionHeader(
                            title = "频道列表",
                            meta = "${state.channels.size} 个频道"
                        )
                    }
                    items(state.channels) { channel ->
                        ChannelRow(
                            channel = channel,
                            onClick = { onNavigateToPlayer(channel) }
                        )
                    }
                }
            }
        }
    }

    if (showSourceSelector) {
        AlertDialog(
            onDismissRequest = { showSourceSelector = false },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("选择直播源") },
            text = {
                Column {
                    sources.filter { it.enabled }.forEach { source ->
                        TextButton(
                            onClick = {
                                viewModel.selectSource(source.id)
                                showSourceSelector = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = source.name,
                                color = if (source.id == currentSourceId) AppColors.Primary else AppColors.TextPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSourceSelector = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun LiveSearchSourceRow(
    searchQuery: String,
    currentSourceName: String,
    onSearchChange: (String) -> Unit,
    onSourceClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("搜索频道") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = AppColors.Primary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppColors.TextPrimary,
                unfocusedTextColor = AppColors.TextPrimary,
                focusedContainerColor = Color.White.copy(alpha = 0.045f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.045f),
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Divider,
                cursorColor = AppColors.Primary,
                focusedPlaceholderColor = AppColors.TextSecondary,
                unfocusedPlaceholderColor = AppColors.TextSecondary
            )
        )
        LiveSourceButton(
            sourceName = currentSourceName,
            onClick = onSourceClick
        )
    }
}

@Composable
private fun LiveSourceButton(
    sourceName: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(58.dp),
        color = Color.White.copy(alpha = 0.045f),
        contentColor = AppColors.TextPrimary,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppColors.Divider)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "切换直播源：$sourceName",
                tint = AppColors.Cream,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun SourceTabs(
    labels: List<String>,
    selected: String?,
    onClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        items(labels.take(10)) { label ->
            val active = label == selected || (selected == null && label == labels.first())
            Surface(
                onClick = { onClick(label) },
                color = if (active) AppColors.Primary else Color.White.copy(alpha = 0.04f),
                contentColor = if (active) AppColors.Background else AppColors.TextSecondary,
                shape = RoundedCornerShape(999.dp),
                border = if (active) null else BorderStroke(1.dp, AppColors.Divider)
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ChannelRow(
    channel: LiveChannel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 4.5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = channel.name.filter { it.isDigit() }.take(2).ifBlank { channel.name.take(1) },
                color = AppColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                color = AppColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${channel.group} · ${channel.format}",
                color = AppColors.TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "LIVE",
            color = AppColors.Error,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.width(4.dp))
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "播放",
            tint = AppColors.Primary,
            modifier = Modifier.size(22.dp)
        )
    }
}
