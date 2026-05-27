package com.zy.player.ui.navigation

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.zy.player.ui.screens.detail.EpisodeListScreen
import com.zy.player.ui.screens.history.HistoryScreen
import com.zy.player.ui.screens.home.HomeScreen
import com.zy.player.ui.screens.live.LiveScreen
import com.zy.player.ui.screens.livesource.LiveSourceManagementScreen
import com.zy.player.ui.screens.online.OnlineScreen
import com.zy.player.ui.screens.player.EpisodePlayerScreen
import com.zy.player.ui.screens.player.LivePlayerScreen
import com.zy.player.ui.screens.search.SearchScreen
import com.zy.player.ui.screens.searchresult.SearchResultScreen
import com.zy.player.ui.screens.settings.SettingsScreen
import com.zy.player.ui.screens.sitemanagement.SiteManagementScreen
import com.zy.player.ui.theme.AppColors

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(Destinations.HOME, Icons.Default.Home, "首页")
    object Live : BottomNavItem(Destinations.LIVE, Icons.Default.LiveTv, "直播")
    object Online : BottomNavItem(Destinations.ONLINE, Icons.Default.Link, "在线")
    object Settings : BottomNavItem(Destinations.SETTINGS, Icons.Default.Settings, "设置")
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Live,
        BottomNavItem.Online,
        BottomNavItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val topLevelRoutes = bottomNavItems.map { it.route }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val activity = context as? Activity
    var onlineClipboardUrl by remember { mutableStateOf<String?>(null) }
    var lastHandledClipboardUrl by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    val prototypeRoutes = bottomNavItems.map { it.route } + listOf(
        Destinations.DETAIL,
        Destinations.EPISODES
    )
    val showBottomBar = currentRoute in prototypeRoutes

    fun handleClipboardPlaylistLink() {
        val clipboardText = runCatching {
            clipboardManager.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(context)
                ?.toString()
        }.getOrNull()
        val playlistUrl = findPlaylistUrlInClipboard(clipboardText) ?: return
        if (playlistUrl == lastHandledClipboardUrl) return

        lastHandledClipboardUrl = playlistUrl
        onlineClipboardUrl = playlistUrl
        navController.navigate(Destinations.ONLINE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    DisposableEffect(clipboardManager) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            handleClipboardPlaylistLink()
        }
        clipboardManager.addPrimaryClipChangedListener(listener)
        onDispose {
            clipboardManager.removePrimaryClipChangedListener(listener)
        }
    }

    DisposableEffect(lifecycleOwner, clipboardManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                handleClipboardPlaylistLink()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        handleClipboardPlaylistLink()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                    onNavigateToPlayer = { channel, sourceId ->
                        navController.navigate(
                            Destinations.livePlayer(
                                url = channel.url,
                                title = channel.name,
                                group = channel.group,
                                format = channel.format,
                                sourceId = sourceId ?: 0L
                            )
                        )
                    }
                )
            }

            composable(Destinations.ONLINE) {
                OnlineScreen(
                    prefillUrl = onlineClipboardUrl,
                    onNavigateToM3u8Player = { url ->
                        navController.navigate(
                            Destinations.episodePlayer(
                                siteId = 0L,
                                vodId = "online",
                                episodeUrl = url,
                                title = "在线播放",
                                episodeLabel = "M3U8"
                            )
                        )
                    },
                    onNavigateToLivePlayer = { channel ->
                        navController.navigate(
                            Destinations.livePlayer(
                                url = channel.url,
                                title = channel.name,
                                group = channel.group,
                                format = channel.format
                            )
                        )
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
                    onNavigateToPlayer = { playerSiteId, playerVodId, episodeUrl, title, episodeLabel ->
                        navController.navigate(
                            Destinations.episodePlayer(playerSiteId, playerVodId, episodeUrl, title, episodeLabel)
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
                            Destinations.episodePlayer(playerSiteId, playerVodId, episodeUrl, title, episodeLabel)
                        )
                    }
                )
            }

            composable(
                route = Destinations.EPISODE_PLAYER,
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
                EpisodePlayerScreen(
                    siteId = siteId,
                    vodId = vodId,
                    episodeUrl = episodeUrl,
                    title = title,
                    episodeLabel = episodeLabel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Destinations.LIVE_PLAYER,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("group") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("format") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("sourceId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url").orEmpty()
                val title = backStackEntry.arguments?.getString("title").orEmpty()
                val group = backStackEntry.arguments?.getString("group").orEmpty()
                val format = backStackEntry.arguments?.getString("format").orEmpty()
                val sourceId = backStackEntry.arguments?.getLong("sourceId") ?: 0L
                LivePlayerScreen(
                    url = url,
                    title = title,
                    group = group,
                    format = format,
                    sourceId = sourceId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Destinations.HISTORY) {
                HistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { siteId, vodId, episodeUrl, title, episodeLabel ->
                        navController.navigate(
                            Destinations.episodePlayer(siteId, vodId, episodeUrl, title, episodeLabel)
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

        BackHandler {
            if (currentRoute in topLevelRoutes) {
                showExitDialog = true
            } else {
                val popped = navController.popBackStack()
                if (!popped) {
                    showExitDialog = true
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("确定要退出吗?") },
            text = { Text("再次确认后将退出应用。") },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("取消")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    }
                ) {
                    Text("退出", color = AppColors.Error)
                }
            }
        )
    }
}

private val PlaylistUrlRegex = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)

private fun findPlaylistUrlInClipboard(text: String?): String? {
    if (text.isNullOrBlank()) return null

    return PlaylistUrlRegex.findAll(text)
        .map { it.value.trimPlaylistUrlBoundary() }
        .firstOrNull { candidate ->
            val normalized = candidate.lowercase()
            normalized.contains(".m3u8") || normalized.contains(".m3u")
        }
}

private fun String.trimPlaylistUrlBoundary(): String {
    return trim().trimEnd(
        ',',
        ';',
        '.',
        ')',
        ']',
        '}',
        '。',
        '，',
        '；'
    )
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
                    val selected = currentRoute == item.route
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
