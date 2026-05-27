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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zy.player.ui.components.CinemaSearchInput
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

    LaunchedEffect(Unit) {
        viewModel.showAllChannels()
    }

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is LiveUiState.Loading -> {
                CinemaLoading(
                    modifier = Modifier.fillMaxSize(),
                    message = "正在解析直播源"
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    item {
                        LiveSearchRow(
                            searchQuery = searchQuery,
                            onSearchChange = viewModel::setSearchQuery
                        )
                    }

                    item {
                        SourceTabs(
                            labels = groups.ifEmpty { listOf("央视", "卫视", "体育", "电影", "少儿") },
                            selected = selectedGroup,
                            currentSourceName = currentSourceName,
                            onSourceClick = { showSourceSelector = true },
                            onAllClick = viewModel::showAllChannels,
                            onClick = { group -> viewModel.selectGroup(if (group == selectedGroup) null else group) }
                        )
                    }

                    when (state) {
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
                        LiveUiState.Loading -> Unit
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
private fun LiveSearchRow(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    CinemaSearchInput(
        value = searchQuery,
        placeholder = "搜索频道",
        onValueChange = onSearchChange,
        modifier = Modifier.padding(top = 0.dp, bottom = 18.dp)
    )
}

@Composable
private fun LiveSourceChip(
    sourceName: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = AppColors.PrimaryLight,
        contentColor = AppColors.Primary,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.34f)),
        modifier = Modifier.width(68.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "切换直播源：$sourceName",
                tint = AppColors.Primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "换源",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SourceTabs(
    labels: List<String>,
    selected: String?,
    currentSourceName: String,
    onSourceClick: () -> Unit,
    onAllClick: () -> Unit,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiveSourceChip(
            sourceName = currentSourceName,
            onClick = onSourceClick
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                SourceTabChip(
                    label = "全部",
                    active = selected == null,
                    onClick = onAllClick
                )
            }
            items(labels.take(10)) { label ->
                SourceTabChip(
                    label = label,
                    active = label == selected,
                    onClick = { onClick(label) }
                )
            }
        }
    }
}

@Composable
private fun SourceTabChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
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
