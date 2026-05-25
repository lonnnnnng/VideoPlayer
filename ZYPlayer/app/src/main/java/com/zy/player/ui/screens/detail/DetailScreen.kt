package com.zy.player.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.zy.player.domain.model.EpisodeGroup
import com.zy.player.domain.model.EpisodeItem
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.components.CinemaLoading
import com.zy.player.ui.components.CinemaMessage
import com.zy.player.ui.components.NetworkImage
import com.zy.player.ui.theme.AppColors

@Composable
fun DetailScreen(
    siteId: Long,
    vodId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEpisodes: (Long, String) -> Unit,
    onNavigateToPlayer: (Long, String, String, String, String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    @Suppress("UNUSED_VARIABLE")
    val unusedRouteArgs = siteId to vodId
    val uiState by viewModel.uiState.collectAsState()

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is DetailUiState.Loading -> CinemaLoading(modifier = Modifier.fillMaxSize())
            is DetailUiState.Error -> CinemaMessage(
                modifier = Modifier.fillMaxSize(),
                title = "详情加载失败",
                message = state.message,
                actionText = "重试",
                onAction = viewModel::loadDetail
            )
            is DetailUiState.Success -> {
                val selectedSource = state.selectedSource
                val vodDetail = selectedSource.vodDetail
                val episodeGroups = selectedSource.episodeGroups
                var selectedGroupName by remember(selectedSource.key) {
                    mutableStateOf(episodeGroups.firstOrNull()?.name)
                }
                val selectedGroup = episodeGroups.firstOrNull { it.name == selectedGroupName }
                    ?: episodeGroups.firstOrNull()
                val continueEpisode = selectedGroup?.episodes?.let(::preferredContinueEpisode)
                var isFavorite by remember(selectedSource.key) { mutableStateOf(false) }
                var showDownloadDialog by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    content = {
                        DetailHero(
                            source = selectedSource,
                            selectedGroup = selectedGroup,
                            continueEpisode = continueEpisode,
                            onBackClick = onNavigateBack,
                            isFavorite = isFavorite,
                            onFavoriteClick = { isFavorite = !isFavorite },
                            onDownloadClick = { showDownloadDialog = true },
                            onPlayClick = {
                                continueEpisode?.let { episode ->
                                    onNavigateToPlayer(
                                        selectedSource.siteId,
                                        selectedSource.vodId,
                                        episode.url,
                                        vodDetail.vod_name,
                                        episode.label
                                    )
                                }
                            }
                        )

                        DetailNoteCard(
                            title = "剧情简介",
                            body = vodDetail.vod_content?.takeIf { it.isNotBlank() }
                                ?.let(::cleanDetailBody)
                                ?: "暂时没有剧情简介，先从剧集列表挑一集开看。"
                        )

                        if (state.sourceOptions.isNotEmpty()) {
                            DetailProviderSection(
                                options = state.sourceOptions,
                                selectedKey = selectedSource.key,
                                onSourceSelect = viewModel::selectSource
                            )
                        }

                        if (episodeGroups.isNotEmpty()) {
                            DetailSourceSection(
                                groups = episodeGroups,
                                selectedGroupName = selectedGroup?.name,
                                onGroupSelect = { selectedGroupName = it }
                            )
                        }

                        DetailEpisodeSummary(
                            totalEpisodes = selectedGroup?.episodes?.size ?: 0,
                            selectedGroupName = selectedGroup?.name ?: "默认线路",
                            onClick = { onNavigateToEpisodes(selectedSource.siteId, selectedSource.vodId) }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                )

                if (showDownloadDialog) {
                    AlertDialog(
                        onDismissRequest = { showDownloadDialog = false },
                        containerColor = AppColors.Surface,
                        titleContentColor = AppColors.TextPrimary,
                        textContentColor = AppColors.TextSecondary,
                        title = { Text("下载") },
                        text = { Text("当前版本已完成在线播放和换源播放，离线下载暂未开放。") },
                        confirmButton = {
                            TextButton(onClick = { showDownloadDialog = false }) {
                                Text("知道了")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHero(
    source: DetailSourceOption,
    selectedGroup: EpisodeGroup?,
    continueEpisode: EpisodeItem?,
    onBackClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(318.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(AppColors.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(25.dp))
    ) {
        NetworkImage(
            url = source.vodDetail.vod_pic,
            contentDescription = source.vodDetail.vod_name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.10f),
                            Color.Transparent,
                            AppColors.Background.copy(alpha = 0.96f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AppColors.Primary.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DetailSmallIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回首页",
                onClick = onBackClick
            )
            DetailSmallIconButton(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                onClick = onFavoriteClick
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailMetaLine(
                items = listOfNotNull(
                    source.siteName,
                    source.vodDetail.vod_remarks?.takeIf { it.isNotBlank() } ?: "在线",
                    source.vodDetail.type_name?.takeIf { it.isNotBlank() },
                    selectedGroup?.episodes?.size?.takeIf { it > 0 }?.let { "更新至 $it" },
                    selectedGroup?.name?.takeIf { it.isNotBlank() }
                )
            )
            Text(
                text = source.vodDetail.vod_name,
                color = AppColors.TextPrimary,
                fontSize = 34.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = source.vodDetail.vod_actor?.takeIf { it.isNotBlank() }
                    ?.let { "主演  $it" }
                    ?: "已解析当前播放线路，可直接播放或切换剧集。",
                color = AppColors.TextPrimary.copy(alpha = 0.72f),
                fontSize = 13.sp,
                lineHeight = 21.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    color = AppColors.Primary,
                    contentColor = AppColors.Background,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(AppColors.Background.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = continueEpisode?.label?.let { "从 $it 继续" } ?: "开始播放",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(width = 46.dp, height = 46.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    contentColor = AppColors.TextPrimary,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "下载",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailProviderSection(
    options: List<DetailSourceOption>,
    selectedKey: String,
    onSourceSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailRailHead(title = "播放源", meta = "自动选择最快线路")

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val selected = option.key == selectedKey
                Surface(
                    onClick = { onSourceSelect(option.key) },
                    color = Color.Transparent,
                    contentColor = if (selected) AppColors.Background else AppColors.TextSecondary,
                    shape = RoundedCornerShape(17.dp),
                    border = if (selected) null else BorderStroke(1.dp, AppColors.Divider)
                ) {
                    Column(
                        modifier = Modifier
                            .width(130.dp)
                            .background(
                                if (selected) {
                                    Brush.linearGradient(listOf(AppColors.Primary, AppColors.Cream))
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.04f),
                                            Color.White.copy(alpha = 0.04f)
                                        )
                                    )
                                }
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = option.siteName,
                            color = if (selected) AppColors.Background else AppColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${option.episodeGroups.sumOf { it.episodes.size }} 集 · ${option.episodeGroups.size} 线",
                            color = if (selected) AppColors.Background else AppColors.TextTertiary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetaLine(items: List<String>) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items.take(4).forEach { item ->
            Text(
                text = item,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                color = AppColors.TextPrimary.copy(alpha = 0.82f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DetailNoteCard(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        AppColors.Accent.copy(alpha = 0.11f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(1.dp, AppColors.Divider, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = AppColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = body,
            color = AppColors.TextPrimary.copy(alpha = 0.72f),
            fontSize = 13.sp,
            lineHeight = 21.sp,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailSourceSection(
    groups: List<EpisodeGroup>,
    selectedGroupName: String?,
    onGroupSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailRailHead(title = "播放线路", meta = "当前资源站内切换")

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groups.forEach { group ->
                val selected = group.name == selectedGroupName
                Surface(
                    onClick = { onGroupSelect(group.name) },
                    color = Color.Transparent,
                    contentColor = if (selected) AppColors.Background else AppColors.TextSecondary,
                    shape = RoundedCornerShape(17.dp),
                    border = if (selected) null else BorderStroke(1.dp, AppColors.Divider)
                ) {
                    Column(
                        modifier = Modifier
                            .width(116.dp)
                            .background(
                                if (selected) {
                                    Brush.linearGradient(listOf(AppColors.Primary, AppColors.Cream))
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.04f),
                                            Color.White.copy(alpha = 0.04f)
                                        )
                                    )
                                }
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = group.name,
                            color = if (selected) AppColors.Background else AppColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${group.episodes.size} 集 · 可切换",
                            color = if (selected) AppColors.Background else AppColors.TextTertiary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailEpisodeSummary(
    totalEpisodes: Int,
    selectedGroupName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(58.dp),
            color = AppColors.Accent,
            contentColor = AppColors.Background,
            shape = RoundedCornerShape(17.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = totalEpisodes.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "剧集列表",
                color = AppColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "$selectedGroupName · 已更新 $totalEpisodes 集",
                color = AppColors.TextTertiary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            contentColor = AppColors.TextSecondary
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "进入剧集列表",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DetailRailHead(
    title: String,
    meta: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
private fun DetailSmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.08f),
        contentColor = AppColors.TextPrimary,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun preferredContinueEpisode(episodes: List<EpisodeItem>): EpisodeItem? {
    if (episodes.isEmpty()) return null
    return episodes.getOrNull(11) ?: episodes.firstOrNull()
}

private fun cleanDetailBody(raw: String): String {
    val plainText = HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    return plainText
        .replace('\u00A0', ' ')
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
        .ifBlank { "暂时没有剧情简介，先从剧集列表挑一集开看。" }
}
