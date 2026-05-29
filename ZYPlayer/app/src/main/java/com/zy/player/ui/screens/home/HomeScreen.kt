package com.zy.player.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.components.CinemaLoading
import com.zy.player.ui.components.CinemaMessage
import com.zy.player.ui.components.CinemaSearchPill
import com.zy.player.ui.components.NetworkImage
import com.zy.player.ui.theme.AppColors

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToDetail: (Long, String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val successState = uiState as? HomeUiState.Success
    val shouldLoadMore by remember(uiState) {
        derivedStateOf {
            val state = uiState as? HomeUiState.Success ?: return@derivedStateOf false
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false

            state.hasMore &&
                !state.isRefreshing &&
                !state.isLoadingMore &&
                !state.isAggregating &&
                layoutInfo.totalItemsCount > 0 &&
                lastVisibleIndex >= layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = successState?.isRefreshing == true,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stickyHeader {
                    HomeStickyHeader(
                        onSearchClick = onNavigateToSearch
                    )
                }

                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        item { CinemaLoading() }
                    }
                    is HomeUiState.Error -> {
                        item {
                            CinemaMessage(
                                title = "片库连接失败",
                                message = state.message,
                                actionText = "重试",
                                onAction = viewModel::refresh
                            )
                        }
                    }
                    is HomeUiState.Empty -> {
                        item {
                            CinemaMessage(
                                title = "暂无影片",
                                message = "当前启用的视频源没有返回内容，试试检查视频源配置。"
                            )
                        }
                    }
                    is HomeUiState.Success -> {
                        state.warningMessage?.let { warning ->
                            item {
                                Text(
                                    text = warning,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
                                    color = AppColors.TextTertiary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        items(
                            items = state.vodList.chunked(3),
                            key = { row -> row.joinToString(separator = "-") { it.key } }
                        ) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach { item ->
                                    CinemaVodPoster(
                                        item = item,
                                        onClick = {
                                            onNavigateToDetail(item.siteId, item.vod.vod_id.toString())
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        if (state.isLoadingMore) {
                            item {
                                HomeLoadMoreFooter(
                                    text = "正在加载更多",
                                    showProgress = true
                                )
                            }
                        } else if (!state.hasMore && state.vodList.isNotEmpty()) {
                            item {
                                HomeLoadMoreFooter(
                                    text = "已经到底了",
                                    showProgress = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeLoadMoreFooter(
    text: String,
    showProgress: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = AppColors.Primary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.padding(horizontal = 5.dp))
        }
        Text(
            text = text,
            color = AppColors.TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HomeStickyHeader(
    onSearchClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .background(AppColors.Shell)
            .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 12.dp)
    ) {
        CinemaSearchPill(
            text = "搜索片名、演员、年份",
            horizontalPadding = 0.dp,
            onClick = onSearchClick
        )
    }
}

@Composable
private fun CinemaVodPoster(
    item: HomeVodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vod = item.vod
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(4.dp))
                .background(AppColors.SurfaceAlt)
                .border(1.dp, AppColors.Divider, RoundedCornerShape(4.dp))
        ) {
            NetworkImage(
                url = vod.vod_pic,
                contentDescription = vod.vod_name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (!vod.vod_remarks.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    color = AppColors.Primary,
                    contentColor = AppColors.OnPrimary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = vod.vod_remarks,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
        Text(
            text = vod.vod_name,
            modifier = Modifier.padding(top = 8.dp),
            color = AppColors.TextPrimary,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = buildHomeVodMeta(item),
            modifier = Modifier.padding(top = 3.dp),
            color = AppColors.TextTertiary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buildHomeVodMeta(item: HomeVodItem): String {
    val baseMeta = listOfNotNull(
        item.vod.type_name,
        item.vod.vod_year
    ).joinToString(" · ")
    val sourceMeta = if (item.sourceCount > 1) {
        item.sourceNames.take(3).joinToString(" / ")
    } else {
        item.siteName
    }
    return listOf(baseMeta, sourceMeta)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { "影视 · 在线" }
}
