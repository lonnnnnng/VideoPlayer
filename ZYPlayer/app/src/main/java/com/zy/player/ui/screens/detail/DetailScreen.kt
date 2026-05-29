package com.zy.player.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
                val detailBody = vodDetail.vod_content?.takeIf { it.isNotBlank() }
                    ?.let(::cleanDetailBody)
                    ?: "暂时没有剧情简介，先从剧集列表挑一集开看。"
                var isFavorite by remember(selectedSource.key) { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = {
                        DetailTopActions(
                            title = vodDetail.vod_name,
                            onBackClick = onNavigateBack,
                            isFavorite = isFavorite,
                            onFavoriteClick = { isFavorite = !isFavorite },
                        )

                        DetailOverviewCard(
                            source = selectedSource,
                            selectedGroup = selectedGroup,
                            body = detailBody
                        )

                        if (state.sourceOptions.isNotEmpty()) {
                            DetailProviderSection(
                                options = state.sourceOptions,
                                selectedKey = selectedSource.key,
                                isLoading = state.isLoadingSources,
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

                        DetailEpisodesSection(
                            episodes = selectedGroup?.episodes.orEmpty(),
                            selectedGroupName = selectedGroup?.name ?: "默认线路",
                            onEpisodeClick = { episode ->
                                onNavigateToPlayer(
                                    selectedSource.siteId,
                                    selectedSource.vodId,
                                    episode.url,
                                    vodDetail.vod_name,
                                    episode.label
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailTopActions(
    title: String,
    onBackClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DetailSmallIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回首页",
            onClick = onBackClick
        )
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp),
            color = AppColors.TextPrimary,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        DetailSmallIconButton(
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "取消收藏" else "收藏",
            onClick = onFavoriteClick
        )
    }
}

@Composable
private fun DetailOverviewCard(
    source: DetailSourceOption,
    selectedGroup: EpisodeGroup?,
    body: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.055f),
                        AppColors.Surface.copy(alpha = 0.82f)
                    )
                )
            )
            .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(
            modifier = Modifier
                .width(116.dp)
                .height(184.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.SurfaceAlt)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
        ) {
            NetworkImage(
                url = source.vodDetail.vod_pic,
                contentDescription = source.vodDetail.vod_name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .height(184.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                fontSize = 22.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = source.vodDetail.vod_actor?.takeIf { it.isNotBlank() }
                    ?.let { "主演  $it" }
                    ?: source.vodDetail.vod_director?.takeIf { it.isNotBlank() }?.let { "导演  $it" }
                    ?: "已解析当前播放线路，可直接播放或切换剧集",
                color = AppColors.TextPrimary.copy(alpha = 0.72f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "简介",
                color = AppColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = body,
                color = AppColors.TextPrimary.copy(alpha = 0.72f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailProviderSection(
    options: List<DetailSourceOption>,
    selectedKey: String,
    isLoading: Boolean,
    onSourceSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRailHead(
            title = "播放源",
            meta = if (isLoading) "正在聚合其他源" else "自动选择最快线路"
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            options.forEach { option ->
                val selected = option.key == selectedKey
                DetailChoicePill(
                    title = option.siteName,
                    meta = "${option.episodeGroups.sumOf { it.episodes.size }}集",
                    selected = selected,
                    minWidth = 86.dp,
                    onClick = { onSourceSelect(option.key) }
                )
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
private fun DetailSourceSection(
    groups: List<EpisodeGroup>,
    selectedGroupName: String?,
    onGroupSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRailHead(title = "播放线路", meta = "当前资源站内切换")

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            groups.forEach { group ->
                val selected = group.name == selectedGroupName
                DetailChoicePill(
                    title = group.name,
                    meta = "${group.episodes.size}集",
                    selected = selected,
                    minWidth = 78.dp,
                    onClick = { onGroupSelect(group.name) }
                )
            }
        }
    }
}

@Composable
private fun DetailChoicePill(
    title: String,
    meta: String,
    selected: Boolean,
    minWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        contentColor = if (selected) AppColors.Background else AppColors.TextSecondary,
        shape = RoundedCornerShape(8.dp),
        border = if (selected) null else BorderStroke(1.dp, AppColors.Divider)
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = minWidth, max = 156.dp)
                .height(32.dp)
                .background(
                    if (selected) {
                        Brush.linearGradient(listOf(AppColors.Primary, AppColors.Primary))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.04f),
                                Color.White.copy(alpha = 0.035f)
                            )
                        )
                    }
                )
                .padding(horizontal = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f, fill = false),
                color = if (selected) AppColors.TextPrimary else AppColors.TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta,
                color = if (selected) AppColors.TextPrimary else AppColors.TextTertiary,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailEpisodesSection(
    episodes: List<EpisodeItem>,
    selectedGroupName: String,
    onEpisodeClick: (EpisodeItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DetailRailHead(
            title = "剧集列表",
            meta = "$selectedGroupName · ${episodes.size}集"
        )

        if (episodes.isEmpty()) {
            Text(
                text = "当前线路暂无可播放剧集",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                color = AppColors.TextTertiary,
                fontSize = 12.sp
            )
            return@Column
        }

        episodes.chunked(5).forEach { rowEpisodes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowEpisodes.forEach { episode ->
                    DetailEpisodeButton(
                        episode = episode,
                        onClick = { onEpisodeClick(episode) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(5 - rowEpisodes.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DetailEpisodeButton(
    episode: EpisodeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(30.dp),
        color = Color.Transparent,
        contentColor = AppColors.TextPrimary,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, AppColors.Divider)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.045f),
                            Color.White.copy(alpha = 0.035f)
                        )
                    )
                )
                .padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = episode.label,
                color = AppColors.TextPrimary,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            color = AppColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = meta,
            color = AppColors.TextTertiary,
            fontSize = 11.sp,
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
        shape = RoundedCornerShape(8.dp),
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
