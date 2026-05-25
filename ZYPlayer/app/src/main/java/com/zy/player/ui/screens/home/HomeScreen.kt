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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.zy.player.data.remote.VodItem
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.components.CinemaLoading
import com.zy.player.ui.components.CinemaMessage
import com.zy.player.ui.components.CinemaSearchPill
import com.zy.player.ui.components.NetworkImage
import com.zy.player.ui.theme.AppColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToDetail: (Long, String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSiteId by viewModel.currentSiteId.collectAsState()
    val gridState = rememberLazyGridState()

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            stickyHeader {
                HomeStickyHeader(
                    meta = when (val success = uiState as? HomeUiState.Success) {
                        null -> "片库同步中"
                        else -> "全部 ${success.vodList.size}"
                    },
                    onSearchClick = onNavigateToSearch
                )
            }

            item {
                when (val state = uiState) {
                    is HomeUiState.Loading -> CinemaLoading()
                    is HomeUiState.Error -> CinemaMessage(
                        title = "片库连接失败",
                        message = state.message,
                        actionText = "重试",
                        onAction = viewModel::refresh
                    )
                    is HomeUiState.Empty -> CinemaMessage(
                        title = "暂无影片",
                        message = "当前分类没有返回内容，试试切换资源站或分类。"
                    )
                    is HomeUiState.Success -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = gridState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(((state.vodList.size.coerceAtMost(18) + 2) / 3 * 202).dp),
                            contentPadding = PaddingValues(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            userScrollEnabled = false
                        ) {
                            items(state.vodList.take(18)) { vod ->
                                CinemaVodPoster(
                                    vod = vod,
                                    onClick = {
                                        currentSiteId?.let { siteId ->
                                            onNavigateToDetail(siteId, vod.vod_id.toString())
                                        }
                                    }
                                )
                            }

                            if (state.hasMore) {
                                item {
                                    LaunchedEffect(Unit) {
                                        viewModel.loadVodList(isRefresh = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStickyHeader(
    meta: String,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppColors.Background,
                        AppColors.Background.copy(alpha = 0.98f),
                        AppColors.Background.copy(alpha = 0.94f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
            )
            .padding(start = 18.dp, top = 0.dp, end = 18.dp, bottom = 12.dp)
    ) {
        CinemaSearchPill(
            text = "搜索电影、剧集、综艺、动漫",
            modifier = Modifier.padding(horizontal = 0.dp),
            horizontalPadding = 0.dp,
            onClick = onSearchClick
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "最近更新",
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
}

@Composable
private fun CinemaVodPoster(
    vod: VodItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(13.dp))
                .background(AppColors.SurfaceAlt)
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(13.dp))
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
                        .padding(7.dp),
                    color = AppColors.Accent,
                    contentColor = Color(0xFF0C0A05),
                    shape = RoundedCornerShape(999.dp)
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
            text = listOfNotNull(vod.type_name, vod.vod_year).joinToString(" · ").ifBlank { "影视 · 在线" },
            modifier = Modifier.padding(top = 3.dp),
            color = AppColors.TextTertiary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
