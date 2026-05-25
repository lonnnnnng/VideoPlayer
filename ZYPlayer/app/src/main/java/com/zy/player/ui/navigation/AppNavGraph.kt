package com.zy.player.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zy.player.ui.screens.detail.DetailScreen
import com.zy.player.ui.screens.history.HistoryScreen
import com.zy.player.ui.screens.home.HomeScreen
import com.zy.player.ui.screens.live.LiveScreen
import com.zy.player.ui.screens.livesource.LiveSourceManagementScreen
import com.zy.player.ui.screens.player.PlayerPreviewScreen
import com.zy.player.ui.screens.player.PlayerScreen
import com.zy.player.ui.screens.search.SearchScreen
import com.zy.player.ui.screens.searchresult.SearchResultScreen
import com.zy.player.ui.screens.settings.SettingsScreen
import com.zy.player.ui.screens.sitemanagement.SiteManagementScreen
import com.zy.player.ui.theme.AppColors
import com.zy.player.ui.screens.detail.EpisodeListScreen

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(Destinations.HOME, Icons.Default.Home, "首页")
    object Live : BottomNavItem(Destinations.LIVE, Icons.Default.LiveTv, "直播")
    object Player : BottomNavItem(Destinations.PLAYER_PREVIEW, Icons.Default.PlayCircle, "播放")
    object Settings : BottomNavItem(Destinations.SETTINGS, Icons.Default.Settings, "设置")
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Live,
        BottomNavItem.Player,
        BottomNavItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val prototypeRoutes = bottomNavItems.map { it.route } + listOf(
        Destinations.DETAIL,
        Destinations.EPISODES,
        Destinations.PLAYER
    )
    val showBottomBar = currentDestination?.route in prototypeRoutes

    Scaffold(
        containerColor = AppColors.Background,
        bottomBar = {
            if (showBottomBar) {
                FloatingCinemaNavigationBar(
                    items = bottomNavItems,
                    currentRoute = currentDestination?.route,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier
                .padding(innerPadding)
                .background(AppColors.Background)
        ) {
            composable(Destinations.HOME) {
                HomeScreen(
                    onNavigateToSearch = { navController.navigate(Destinations.SEARCH) },
                    onNavigateToDetail = { siteId, vodId ->
                        navController.navigate(Destinations.detail(siteId, vodId))
                    }
                )
            }

            composable(Destinations.LIVE) {
                LiveScreen(
                    onNavigateToPlayer = { url ->
                        navController.navigate(Destinations.player(0, "live", url))
                    }
                )
            }

            composable(Destinations.SETTINGS) {
                SettingsScreen(
                    onNavigateToHistory = { navController.navigate(Destinations.HISTORY) },
                    onNavigateToSiteManagement = { navController.navigate(Destinations.SITE_MANAGEMENT) },
                    onNavigateToLiveSourceManagement = { navController.navigate(Destinations.LIVE_SOURCE_MANAGEMENT) }
                )
            }

            composable(Destinations.PLAYER_PREVIEW) {
                PlayerPreviewScreen()
            }

            composable(Destinations.SEARCH) {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSearchResult = { keyword ->
                        navController.navigate(Destinations.searchResult(keyword))
                    }
                )
            }

            composable(
                route = Destinations.SEARCH_RESULT,
                arguments = listOf(navArgument("keyword") { type = NavType.StringType })
            ) { backStackEntry ->
                val keyword = Uri.decode(backStackEntry.arguments?.getString("keyword") ?: "")
                SearchResultScreen(
                    keyword = keyword,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { siteId, vodId ->
                        navController.navigate(Destinations.detail(siteId, vodId))
                    }
                )
            }

            composable(
                route = Destinations.DETAIL,
                arguments = listOf(
                    navArgument("siteId") { type = NavType.LongType },
                    navArgument("vodId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val siteId = backStackEntry.arguments?.getLong("siteId") ?: 0L
                val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
                DetailScreen(
                    siteId = siteId,
                    vodId = vodId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEpisodes = { detailSiteId, detailVodId ->
                        navController.navigate(Destinations.episodes(detailSiteId, detailVodId))
                    },
                    onNavigateToPlayer = { playerSiteId, playerVodId, episodeUrl, title, episodeLabel ->
                        navController.navigate(
                            Destinations.player(playerSiteId, playerVodId, episodeUrl, title, episodeLabel)
                        )
                    }
                )
            }

            composable(
                route = Destinations.EPISODES,
                arguments = listOf(
                    navArgument("siteId") { type = NavType.LongType },
                    navArgument("vodId") { type = NavType.StringType }
                )
            ) {
                EpisodeListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { playerSiteId, playerVodId, episodeUrl, title, episodeLabel ->
                        navController.navigate(
                            Destinations.player(playerSiteId, playerVodId, episodeUrl, title, episodeLabel)
                        )
                    }
                )
            }

            composable(
                route = Destinations.PLAYER,
                arguments = listOf(
                    navArgument("siteId") { type = NavType.LongType },
                    navArgument("vodId") { type = NavType.StringType },
                    navArgument("episodeUrl") { type = NavType.StringType },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("episodeLabel") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val siteId = backStackEntry.arguments?.getLong("siteId") ?: 0L
                val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
                val episodeUrl = backStackEntry.arguments?.getString("episodeUrl") ?: ""
                val title = backStackEntry.arguments?.getString("title").orEmpty()
                val episodeLabel = backStackEntry.arguments?.getString("episodeLabel").orEmpty()
                PlayerScreen(
                    siteId = siteId,
                    vodId = vodId,
                    episodeUrl = episodeUrl,
                    title = title,
                    episodeLabel = episodeLabel
                )
            }

            composable(Destinations.HISTORY) {
                HistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { siteId, vodId, episodeUrl, title, episodeLabel ->
                        navController.navigate(
                            Destinations.player(siteId, vodId, episodeUrl, title, episodeLabel)
                        )
                    }
                )
            }

            composable(Destinations.SITE_MANAGEMENT) {
                SiteManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Destinations.LIVE_SOURCE_MANAGEMENT) {
                LiveSourceManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun FloatingCinemaNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        AppColors.Background.copy(alpha = 0.94f),
                        AppColors.Background,
                        AppColors.Background
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f))
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFA181D1D),
                            Color(0xFA0D1111)
                        )
                    )
                )
                .border(1.dp, AppColors.Divider, RoundedCornerShape(24.dp))
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route ||
                        (item.route == Destinations.PLAYER_PREVIEW && currentRoute == Destinations.PLAYER)
                    Surface(
                        onClick = { onItemClick(item) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        color = if (selected) AppColors.Cream else Color.Transparent,
                        contentColor = if (selected) AppColors.Background else AppColors.TextSecondary,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
