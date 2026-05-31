package com.zy.player.ui.screens.audio

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zy.player.domain.model.PodcastEpisode
import com.zy.player.domain.model.RadioStation
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.screens.podcast.PodcastScreen
import com.zy.player.ui.screens.podcast.PodcastViewModel
import com.zy.player.ui.screens.radio.RadioScreen
import com.zy.player.ui.theme.AppColors

private enum class AudioTab(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    Radio("电台", "直播音频", Icons.Default.Radio),
    Podcast("播客", "订阅节目", Icons.Default.Podcasts)
}

@Composable
fun AudioScreen(
    onNavigateToRadioPlayer: (RadioStation, Long?) -> Unit,
    onNavigateToPodcastPlayer: (PodcastEpisode, String, String) -> Unit,
    podcastViewModel: PodcastViewModel = hiltViewModel()
) {
    var selectedTabName by rememberSaveable { mutableStateOf(AudioTab.Radio.name) }
    val selectedTab = AudioTab.valueOf(selectedTabName)
    val podcastUiState by podcastViewModel.uiState.collectAsState()

    LaunchedEffect(selectedTab) {
        if (selectedTab == AudioTab.Podcast) {
            podcastViewModel.refreshLibraryIfNeeded()
        }
    }

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AudioHeader(
                selectedTab = selectedTab,
                isPodcastRefreshing = podcastUiState.isRefreshingLibrary,
                onTabSelected = { selectedTabName = it.name },
                onPodcastRefreshClick = podcastViewModel::refreshLibrary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Crossfade(targetState = selectedTab, label = "audio-tab-content") { tab ->
                    when (tab) {
                        AudioTab.Radio -> RadioScreen(
                            onNavigateToPlayer = onNavigateToRadioPlayer,
                            useOuterBackground = false
                        )
                        AudioTab.Podcast -> PodcastScreen(
                            onNavigateBack = {},
                            onNavigateToPlayer = onNavigateToPodcastPlayer,
                            useOuterBackground = false,
                            showBackButton = false,
                            showHeader = false,
                            contentBottomPadding = 18.dp,
                            viewModel = podcastViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioHeader(
    selectedTab: AudioTab,
    isPodcastRefreshing: Boolean,
    onTabSelected: (AudioTab) -> Unit,
    onPodcastRefreshClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Shell)
            .padding(start = 14.dp, top = 6.dp, end = 14.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "音频",
                    color = AppColors.TextPrimary,
                    fontSize = 21.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "电台和播客统一入口",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (selectedTab == AudioTab.Podcast) {
                IconButton(
                    onClick = onPodcastRefreshClick,
                    enabled = !isPodcastRefreshing,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppColors.PrimaryLight)
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新播客节目",
                        tint = if (isPodcastRefreshing) AppColors.TextTertiary else AppColors.Primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            } else {
                Text(
                    text = selectedTab.subtitle,
                    color = AppColors.Primary,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppColors.Divider, RoundedCornerShape(6.dp)),
            color = AppColors.Surface,
            shape = RoundedCornerShape(6.dp),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AudioTab.values().forEach { tab ->
                    AudioTabButton(
                        tab = tab,
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioTabButton(
    tab: AudioTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) AppColors.Primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.title,
            tint = if (selected) AppColors.OnPrimary else AppColors.TextSecondary,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(18.dp)
        )
        Text(
            text = tab.title,
            color = if (selected) AppColors.OnPrimary else AppColors.TextPrimary,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Black
        )
    }
}
