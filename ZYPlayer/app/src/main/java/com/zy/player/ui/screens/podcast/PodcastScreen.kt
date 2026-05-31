package com.zy.player.ui.screens.podcast

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zy.player.data.local.entity.PodcastSubscriptionEntity
import com.zy.player.domain.model.PodcastEpisode
import com.zy.player.domain.model.PodcastFeed
import com.zy.player.domain.model.PodcastLibraryEpisode
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.components.CinemaMessage
import com.zy.player.ui.components.NetworkImage
import com.zy.player.ui.components.PageHeader
import com.zy.player.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PodcastScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (PodcastEpisode, String, String) -> Unit,
    useOuterBackground: Boolean = true,
    showBackButton: Boolean = true,
    showHeader: Boolean = true,
    contentBottomPadding: Dp = 96.dp,
    viewModel: PodcastViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    var inputUrl by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<PodcastSubscriptionEntity?>(null) }

    BackHandler(enabled = uiState.selectedSubscriptionId != null) {
        viewModel.closeSubscription()
    }

    val content: @Composable () -> Unit = {
        if (uiState.selectedSubscriptionId != null) {
            PodcastSubscriptionDetailPage(
                uiState = uiState,
                onBackClick = viewModel::closeSubscription,
                onEpisodeClick = { episode, feed ->
                    onNavigateToPlayer(episode, feed.title, feed.imageUrl)
                },
                contentBottomPadding = contentBottomPadding
            )
        } else {
            PodcastLibraryPage(
                inputUrl = inputUrl,
                onInputChange = { inputUrl = it },
                uiState = uiState,
                subscriptions = subscriptions,
                showHeader = showHeader,
                contentBottomPadding = contentBottomPadding,
                onBackClick = if (showBackButton) onNavigateBack else null,
                onAddClick = {
                    viewModel.addSubscription(inputUrl)
                    inputUrl = ""
                },
                onRefreshClick = viewModel::refreshLibrary,
                onSubscriptionClick = viewModel::openSubscription,
                onDeleteClick = { pendingDelete = it },
                onEpisodeClick = { libraryEpisode ->
                    onNavigateToPlayer(
                        libraryEpisode.episode,
                        libraryEpisode.feedTitle,
                        libraryEpisode.feedImageUrl
                    )
                }
            )
        }
    }

    if (useOuterBackground) {
        CinemaBackground(modifier = Modifier.fillMaxSize()) {
            content()
        }
    } else {
        content()
    }

    uiState.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearMessage) {
                    Text("知道了")
                }
            }
        )
    }

    pendingDelete?.let { subscription ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("删除订阅") },
            text = { Text("确定删除 ${subscription.title} 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSubscription(subscription)
                        pendingDelete = null
                    }
                ) {
                    Text("删除", color = AppColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun PodcastLibraryPage(
    inputUrl: String,
    onInputChange: (String) -> Unit,
    uiState: PodcastUiState,
    subscriptions: List<PodcastSubscriptionEntity>,
    showHeader: Boolean,
    contentBottomPadding: Dp,
    onBackClick: (() -> Unit)?,
    onAddClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSubscriptionClick: (PodcastSubscriptionEntity) -> Unit,
    onDeleteClick: (PodcastSubscriptionEntity) -> Unit,
    onEpisodeClick: (PodcastLibraryEpisode) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val canAdd = inputUrl.trim().startsWith("http://") || inputUrl.trim().startsWith("https://")

    Column(modifier = Modifier.fillMaxSize()) {
        if (showHeader) {
            PageHeader(
                title = "播客",
                onBackClick = onBackClick,
                actions = {
                    HeaderActionButton(
                        icon = Icons.Default.Refresh,
                        contentDescription = "刷新聚合节目",
                        enabled = !uiState.isRefreshingLibrary,
                        onClick = onRefreshClick
                    )
                }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, top = 8.dp, end = 14.dp, bottom = contentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PodcastIntroCard(subscriptionCount = subscriptions.size)
            }

            item {
                PodcastAddCard(
                    inputUrl = inputUrl,
                    isAdding = uiState.isAdding,
                    canAdd = canAdd,
                    onInputChange = onInputChange,
                    onAddClick = {
                        focusManager.clearFocus(force = true)
                        onAddClick()
                    }
                )
            }

            item {
                SectionTitle(title = "我的订阅", subtitle = "${subscriptions.size} 个")
            }

            if (subscriptions.isEmpty()) {
                item {
                    CinemaMessage(
                        title = "还没有订阅",
                        message = "粘贴播客 RSS 地址后添加订阅，聚合首页会自动显示最新节目。"
                    )
                }
            } else {
                items(
                    items = subscriptions,
                    key = { it.id },
                    contentType = { "podcast-subscription-row" }
                ) { subscription ->
                    PodcastSubscriptionRow(
                        subscription = subscription,
                        onClick = { onSubscriptionClick(subscription) },
                        onDeleteClick = { onDeleteClick(subscription) }
                    )
                }
            }

            item {
                SectionTitle(
                    title = "最新节目",
                    subtitle = if (uiState.isRefreshingLibrary) "刷新中" else "${uiState.libraryEpisodes.size} 期"
                )
            }

            if (uiState.isRefreshingLibrary && uiState.libraryEpisodes.isEmpty()) {
                item { PodcastLoadingCard(message = "正在聚合最新节目") }
            } else if (uiState.libraryEpisodes.isEmpty()) {
                item {
                    CinemaMessage(
                        title = "暂无聚合节目",
                        message = "添加订阅后，这里会按照发布时间聚合展示最新节目。"
                    )
                }
            } else {
                items(
                    items = uiState.libraryEpisodes,
                    key = { item -> "${item.subscriptionId}|${item.episode.audioUrl}" },
                    contentType = { "podcast-library-episode-row" }
                ) { item ->
                    PodcastLibraryEpisodeRow(
                        item = item,
                        onClick = { onEpisodeClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastSubscriptionDetailPage(
    uiState: PodcastUiState,
    onBackClick: () -> Unit,
    onEpisodeClick: (PodcastEpisode, PodcastFeed) -> Unit,
    contentBottomPadding: Dp
) {
    val feed = uiState.selectedFeed
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(
            title = feed?.title ?: "订阅详情",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = contentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.isLoadingFeed || feed == null) {
                item { PodcastLoadingCard(message = "正在刷新订阅节目") }
            } else {
                item { PodcastFeedHeader(feed = feed) }
                items(
                    items = feed.episodes,
                    key = { episode -> episode.audioUrl },
                    contentType = { "podcast-episode-row" }
                ) { episode ->
                    PodcastEpisodeRow(
                        episode = episode,
                        onClick = { onEpisodeClick(episode, feed) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastIntroCard(subscriptionCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(4.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AppColors.PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Podcasts,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "播客订阅",
                color = AppColors.TextPrimary,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (subscriptionCount > 0) {
                    "已订阅 $subscriptionCount 个播客，最新节目会在下方展示。"
                } else {
                    "添加 RSS 订阅后，这里会展示所有播客的最新节目。"
                },
                color = AppColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun PodcastAddCard(
    inputUrl: String,
    isAdding: Boolean,
    canAdd: Boolean,
    onInputChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(4.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = inputUrl,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp, max = 116.dp),
            label = { Text("RSS 订阅地址") },
            placeholder = {
                Text(
                    text = "https://example.com/podcast/rss.xml",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            textStyle = TextStyle(
                color = AppColors.TextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppColors.TextPrimary,
                unfocusedTextColor = AppColors.TextPrimary,
                focusedContainerColor = AppColors.Surface,
                unfocusedContainerColor = AppColors.Surface,
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Divider,
                cursorColor = AppColors.Primary,
                focusedLabelColor = AppColors.Primary,
                unfocusedLabelColor = AppColors.TextSecondary,
                focusedPlaceholderColor = AppColors.TextTertiary,
                unfocusedPlaceholderColor = AppColors.TextTertiary
            )
        )

        Surface(
            onClick = onAddClick,
            enabled = canAdd && !isAdding,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            color = if (canAdd && !isAdding) AppColors.Primary else AppColors.SurfaceRaised,
            contentColor = if (canAdd && !isAdding) AppColors.OnPrimary else AppColors.TextTertiary,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, if (canAdd && !isAdding) Color.Transparent else AppColors.Divider)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAdding) Icons.Default.RssFeed else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAdding) "正在订阅" else "添加订阅",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = AppColors.TextPrimary,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = subtitle,
            color = AppColors.TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PodcastSubscriptionRow(
    subscription: PodcastSubscriptionEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PodcastCover(
            imageUrl = subscription.imageUrl,
            title = subscription.title,
            size = 52
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = subscription.title,
                color = AppColors.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${subscription.episodeCount} 期 · ${formatRefreshTime(subscription.lastRefreshTime)}",
                color = AppColors.TextTertiary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subscription.url,
                color = AppColors.TextTertiary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除订阅",
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PodcastFeedHeader(feed: PodcastFeed) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(4.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PodcastCover(imageUrl = feed.imageUrl, title = feed.title, size = 72)
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = feed.title,
                color = AppColors.TextPrimary,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${feed.episodes.size} 期节目",
                color = AppColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (feed.description.isNotBlank()) {
                Text(
                    text = feed.description,
                    color = AppColors.TextTertiary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PodcastLibraryEpisodeRow(
    item: PodcastLibraryEpisode,
    onClick: () -> Unit
) {
    PodcastEpisodeBaseRow(
        title = item.episode.title,
        subtitle = item.feedTitle,
        meta = listOf(item.episode.duration, item.episode.pubDate).filter { it.isNotBlank() }.joinToString(" · "),
        imageUrl = item.episode.imageUrl.ifBlank { item.feedImageUrl },
        onClick = onClick
    )
}

@Composable
private fun PodcastEpisodeRow(
    episode: PodcastEpisode,
    onClick: () -> Unit
) {
    PodcastEpisodeBaseRow(
        title = episode.title,
        subtitle = episode.description,
        meta = listOf(episode.duration, episode.pubDate).filter { it.isNotBlank() }.joinToString(" · "),
        imageUrl = episode.imageUrl,
        onClick = onClick
    )
}

@Composable
private fun PodcastEpisodeBaseRow(
    title: String,
    subtitle: String,
    meta: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageUrl.isNotBlank()) {
            PodcastCover(imageUrl = imageUrl, title = title, size = 48)
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                color = AppColors.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = AppColors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    color = AppColors.TextTertiary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "播放节目",
            tint = AppColors.Primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun PodcastCover(imageUrl: String, title: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.SurfaceAlt)
    ) {
        NetworkImage(
            url = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PodcastLoadingCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = AppColors.Primary,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            color = AppColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(4.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) AppColors.Primary else AppColors.TextTertiary,
            modifier = Modifier.size(21.dp)
        )
    }
}

private fun formatRefreshTime(timestamp: Long): String {
    if (timestamp <= 0L) return "未刷新"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
