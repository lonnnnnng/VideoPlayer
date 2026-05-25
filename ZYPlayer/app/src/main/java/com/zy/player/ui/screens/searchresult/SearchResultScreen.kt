package com.zy.player.ui.screens.searchresult

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.zy.player.ui.components.*
import com.zy.player.ui.theme.Dimens

@Composable
fun SearchResultScreen(
    keyword: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long, String) -> Unit,
    viewModel: SearchResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(
            title = "搜索: $keyword",
            onBackClick = onNavigateBack
        )

        StateLayout(
            isLoading = uiState is SearchResultUiState.Loading,
            isError = uiState is SearchResultUiState.Error,
            isEmpty = uiState is SearchResultUiState.Empty,
            errorMessage = (uiState as? SearchResultUiState.Error)?.message,
            emptyMessage = "未找到相关内容",
            onRetry = { viewModel.search() }
        ) {
            val successState = uiState as? SearchResultUiState.Success
            if (successState != null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(Dimens.paddingMedium),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.paddingMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.paddingMedium)
                ) {
                    items(successState.vodList) { item ->
                        val vod = item.vod
                        VodCard(
                            title = vod.vod_name,
                            coverUrl = vod.vod_pic,
                            remark = vod.vod_remarks,
                            sourceName = item.siteName,
                            onClick = {
                                onNavigateToDetail(item.siteId, vod.vod_id.toString())
                            }
                        )
                    }

                    if (successState.hasMore) {
                        item {
                            LaunchedEffect(Unit) {
                                viewModel.loadMore()
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
