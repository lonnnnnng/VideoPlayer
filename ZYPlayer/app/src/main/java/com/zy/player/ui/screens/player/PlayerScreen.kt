package com.zy.player.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.theme.AppColors
import kotlin.math.abs

private const val QUICK_SEEK_MS = 10_000L

@Composable
fun EpisodePlayerScreen(
    siteId: Long,
    vodId: String,
    episodeUrl: String,
    title: String,
    episodeLabel: String,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    @Suppress("UNUSED_VARIABLE")
    val unusedRouteArgs = Triple(siteId, vodId, episodeUrl)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val activity = context as? Activity

    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val activeEpisodeUrl by viewModel.activeEpisodeUrl.collectAsState()
    val playbackUiState by viewModel.playbackUiState.collectAsState()
    val sourceOptions by viewModel.sourceOptions.collectAsState()
    val playbackStats by viewModel.playbackStats.collectAsState()

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showCastDialog by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var brightnessOverlay by remember { mutableStateOf<Float?>(null) }
    var volumeOverlay by remember { mutableStateOf<Int?>(null) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    val seekBackward = {
        viewModel.seekTo(quickSeekPosition(currentPosition, duration, -QUICK_SEEK_MS))
    }
    val seekForward = {
        viewModel.seekTo(quickSeekPosition(currentPosition, duration, QUICK_SEEK_MS))
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.savePlaybackPosition(title, "", episodeLabel)
            viewModel.stopPlayback()
            if (isFullscreen) {
                exitFullscreen(activity)
            }
        }
    }

    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            enterFullscreen(activity)
        } else {
            exitFullscreen(activity)
        }
        onDispose { }
    }

    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            showControls = true
        }
    }

    val playerViewFactory: @Composable () -> Unit = {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            onRelease = { playerView ->
                playerView.player = null
            },
            update = { playerView ->
                playerView.player = viewModel.getPlayer()
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isFullscreen) {
                    PlayerEpisodeTopBar(
                        title = episodeTopBarTitle(title, episodeLabel),
                        onNavigateBack = onNavigateBack
                    )

                    PlayerSurface(
                        isFullscreen = false,
                        showControls = showControls,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        playbackUiState = playbackUiState,
                        brightnessOverlay = brightnessOverlay,
                        volumeOverlay = volumeOverlay,
                        maxVolume = maxVolume,
                        onToggleControls = { showControls = !showControls },
                        onTogglePlay = viewModel::togglePlayPause,
                        onRetryPlayback = viewModel::retryPlayback,
                        onSeekTo = viewModel::seekTo,
                        onToggleFullscreen = {
                            showControls = true
                            isFullscreen = true
                        },
                        onRewindClick = seekBackward,
                        onForwardClick = seekForward,
                        onExitFullscreen = { isFullscreen = false },
                        onBrightnessChange = { brightnessOverlay = it },
                        onVolumeChange = { volumeOverlay = it },
                        onGestureEnd = {
                            brightnessOverlay = null
                            volumeOverlay = null
                        },
                        audioManager = audioManager,
                        activity = activity,
                        context = context,
                        playerViewFactory = playerViewFactory
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = episodePlayerSubtitle(
                            episodeUrl = activeEpisodeUrl,
                            currentPosition = currentPosition,
                            duration = duration,
                            playbackUiState = playbackUiState
                        ),
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    LinearProgressIndicator(
                        progress = {
                            if (duration > 0) currentPosition.toFloat() / duration else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = AppColors.Primary,
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayerMetaPill(formatTime(currentPosition))
                        PlayerMetaPill(playerSourceLabel(activeEpisodeUrl))
                        PlayerMetaPill(
                            if (duration > 0L) {
                                "${((currentPosition * 100f) / duration).toInt()}%"
                            } else {
                                "准备中"
                            }
                        )
                    }

                    PlaybackStatsRow(stats = playbackStats)

                    PlayerSourceLinkRow(
                        url = activeEpisodeUrl,
                        onCopyClick = {
                            copyPlayerSourceLink(
                                context = context,
                                clipboardManager = clipboardManager,
                                url = activeEpisodeUrl
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerUtilityButton(
                            icon = Icons.Default.FastRewind,
                            label = "快退",
                            onClick = seekBackward,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(13.dp)
                        )

                        Surface(
                            onClick = {
                                if (playbackUiState.isFailed) {
                                    viewModel.retryPlayback()
                                } else {
                                    viewModel.togglePlayPause()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            color = AppColors.SurfaceAlt,
                            contentColor = AppColors.TextPrimary,
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, AppColors.Divider)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = when {
                                        playbackUiState.isFailed -> "重试"
                                        isPlaying -> "暂停"
                                        else -> "播放"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    softWrap = false
                                )
                            }
                        }

                        PlayerUtilityButton(
                            icon = Icons.Default.FastForward,
                            label = "快进",
                            onClick = seekForward,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(13.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerUtilityButton(
                            icon = Icons.Default.SwapHoriz,
                            label = "换源",
                            onClick = {
                                viewModel.loadSourceOptions()
                                showSourceDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(13.dp)
                        )

                        PlayerUtilityButton(
                            icon = Icons.Default.Speed,
                            label = "${playbackSpeed}x",
                            onClick = { showSpeedDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(13.dp)
                        )

                        PlayerUtilityButton(
                            icon = Icons.Default.Cast,
                            label = "投屏",
                            onClick = { showCastDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(13.dp)
                        )

                        PlayerUtilityButton(
                            icon = Icons.Default.Fullscreen,
                            label = "全屏",
                            onClick = {
                                showControls = true
                                isFullscreen = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(13.dp)
                        )
                    }
                }
            }

            if (isFullscreen) {
                PlayerSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f),
                    isFullscreen = true,
                    showControls = showControls,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    playbackUiState = playbackUiState,
                    brightnessOverlay = brightnessOverlay,
                    volumeOverlay = volumeOverlay,
                    maxVolume = maxVolume,
                    onToggleControls = { showControls = !showControls },
                    onTogglePlay = viewModel::togglePlayPause,
                    onRetryPlayback = viewModel::retryPlayback,
                    onSeekTo = viewModel::seekTo,
                    onToggleFullscreen = { isFullscreen = false },
                    onRewindClick = seekBackward,
                    onForwardClick = seekForward,
                    onSourceClick = {
                        viewModel.loadSourceOptions()
                        showSourceDialog = true
                    },
                    onSpeedClick = { showSpeedDialog = true },
                    playbackSpeedLabel = "${playbackSpeed}x",
                    onCastClick = { showCastDialog = true },
                    onExitFullscreen = { isFullscreen = false },
                    onBrightnessChange = { brightnessOverlay = it },
                    onVolumeChange = { volumeOverlay = it },
                    onGestureEnd = {
                        brightnessOverlay = null
                        volumeOverlay = null
                    },
                    audioManager = audioManager,
                    activity = activity,
                    context = context,
                    playerViewFactory = playerViewFactory
                )
            }
        }
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("播放速度") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        0.5f,
                        0.75f,
                        1.0f,
                        1.25f,
                        1.5f,
                        2.0f,
                        2.5f,
                        3.0f,
                        4.0f
                    ).chunked(3).forEach { rowSpeeds ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowSpeeds.forEach { speed ->
                                TextButton(
                                    onClick = {
                                        viewModel.setSpeed(speed)
                                        showSpeedDialog = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        color = if (speed == playbackSpeed) AppColors.Primary else AppColors.TextPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showCastDialog) {
        AlertDialog(
            onDismissRequest = { showCastDialog = false },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("投屏") },
            text = { Text("当前未发现可用投屏设备，请确认电视或盒子与手机在同一网络。") },
            confirmButton = {
                TextButton(onClick = { showCastDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("播放换源") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (sourceOptions.isEmpty()) {
                        Text("正在查找同集可用线路...")
                    } else {
                        sourceOptions.forEach { option ->
                            TextButton(
                                onClick = {
                                    viewModel.switchToSource(option.key)
                                    showSourceDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = option.sourceName,
                                            color = if (option.isCurrent) AppColors.Primary else AppColors.TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = option.episodeLabel.ifBlank { episodeLabel.ifBlank { "当前集" } },
                                            color = AppColors.TextTertiary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (option.isCurrent) {
                                        Text(
                                            text = "当前",
                                            color = AppColors.Primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSourceDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
fun LivePlayerScreen(
    url: String,
    title: String,
    group: String,
    format: String,
    onNavigateBack: () -> Unit,
    viewModel: LivePlayerViewModel = hiltViewModel()
) {
    @Suppress("UNUSED_VARIABLE")
    val unusedRouteArgs = url to group
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val activity = context as? Activity

    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val activeLiveUrl by viewModel.activeLiveUrl.collectAsState()
    val playbackUiState by viewModel.playbackUiState.collectAsState()
    val playbackStats by viewModel.playbackStats.collectAsState()

    var showCastDialog by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var brightnessOverlay by remember { mutableStateOf<Float?>(null) }
    var volumeOverlay by remember { mutableStateOf<Int?>(null) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPlayback()
            if (isFullscreen) {
                exitFullscreen(activity)
            }
        }
    }

    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            enterFullscreen(activity)
        } else {
            exitFullscreen(activity)
        }
        onDispose { }
    }

    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            showControls = true
        }
    }

    val playerViewFactory: @Composable () -> Unit = {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            onRelease = { playerView ->
                playerView.player = null
            },
            update = { playerView ->
                playerView.player = viewModel.getPlayer()
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isFullscreen) {
                    PlayerEpisodeTopBar(
                        title = title.ifBlank { "直播频道" },
                        onNavigateBack = onNavigateBack
                    )

                    PlayerSurface(
                        isFullscreen = false,
                        showControls = showControls,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        playbackUiState = playbackUiState,
                        recoveringTitle = "正在连接直播流",
                        brightnessOverlay = brightnessOverlay,
                        volumeOverlay = volumeOverlay,
                        maxVolume = maxVolume,
                        onToggleControls = { showControls = !showControls },
                        onTogglePlay = viewModel::togglePlayPause,
                        onRetryPlayback = viewModel::retryPlayback,
                        onSeekTo = viewModel::seekTo,
                        onToggleFullscreen = {
                            showControls = true
                            isFullscreen = true
                        },
                        onExitFullscreen = { isFullscreen = false },
                        onBrightnessChange = { brightnessOverlay = it },
                        onVolumeChange = { volumeOverlay = it },
                        onGestureEnd = {
                            brightnessOverlay = null
                            volumeOverlay = null
                        },
                        audioManager = audioManager,
                        activity = activity,
                        context = context,
                        playerViewFactory = playerViewFactory
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = livePlayerSubtitle(
                            group = group,
                            format = format,
                            liveUrl = activeLiveUrl,
                            playbackUiState = playbackUiState
                        ),
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    LinearProgressIndicator(
                        progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = AppColors.Primary,
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayerMetaPill("直播")
                        PlayerMetaPill(playerSourceLabel(activeLiveUrl))
                        PlayerMetaPill(format.ifBlank { "IPTV" })
                    }

                    PlaybackStatsRow(stats = playbackStats)

                    PlayerSourceLinkRow(
                        url = activeLiveUrl,
                        onCopyClick = {
                            copyPlayerSourceLink(
                                context = context,
                                clipboardManager = clipboardManager,
                                url = activeLiveUrl
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerUtilityButton(
                            icon = Icons.Default.Cast,
                            label = "投屏",
                            onClick = { showCastDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(13.dp)
                        )

                        Surface(
                            onClick = {
                                if (playbackUiState.isFailed) {
                                    viewModel.retryPlayback()
                                } else {
                                    viewModel.togglePlayPause()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            color = AppColors.SurfaceAlt,
                            contentColor = AppColors.TextPrimary,
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, AppColors.Divider)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = when {
                                        playbackUiState.isFailed -> "重试直播"
                                        isPlaying -> "暂停"
                                        else -> "播放"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    softWrap = false
                                )
                            }
                        }

                        PlayerUtilityButton(
                            icon = Icons.Default.Fullscreen,
                            label = "全屏",
                            onClick = {
                                showControls = true
                                isFullscreen = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(13.dp)
                        )
                    }
                }
            }

            if (isFullscreen) {
                PlayerSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f),
                    isFullscreen = true,
                    showControls = showControls,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    playbackUiState = playbackUiState,
                    recoveringTitle = "正在连接直播流",
                    brightnessOverlay = brightnessOverlay,
                    volumeOverlay = volumeOverlay,
                    maxVolume = maxVolume,
                    onToggleControls = { showControls = !showControls },
                    onTogglePlay = viewModel::togglePlayPause,
                    onRetryPlayback = viewModel::retryPlayback,
                    onSeekTo = viewModel::seekTo,
                    onToggleFullscreen = { isFullscreen = false },
                    onCastClick = { showCastDialog = true },
                    onExitFullscreen = { isFullscreen = false },
                    onBrightnessChange = { brightnessOverlay = it },
                    onVolumeChange = { volumeOverlay = it },
                    onGestureEnd = {
                        brightnessOverlay = null
                        volumeOverlay = null
                    },
                    audioManager = audioManager,
                    activity = activity,
                    context = context,
                    playerViewFactory = playerViewFactory
                )
            }
        }
    }

    if (showCastDialog) {
        AlertDialog(
            onDismissRequest = { showCastDialog = false },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("投屏") },
            text = { Text("当前未发现可用投屏设备，请确认电视或盒子与手机在同一网络。") },
            confirmButton = {
                TextButton(onClick = { showCastDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }
}

@Composable
private fun PlayerEpisodeTopBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 10.dp, end = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onNavigateBack,
            modifier = Modifier.size(42.dp),
            color = AppColors.SurfaceAlt,
            contentColor = AppColors.TextPrimary,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, AppColors.Divider)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            color = AppColors.TextPrimary,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlayerSurface(
    modifier: Modifier = Modifier,
    isFullscreen: Boolean,
    showControls: Boolean,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    playbackUiState: PlaybackUiState,
    recoveringTitle: String = "正在切换备用线路",
    brightnessOverlay: Float?,
    volumeOverlay: Int?,
    maxVolume: Int,
    onToggleControls: () -> Unit,
    onTogglePlay: () -> Unit,
    onRetryPlayback: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFullscreen: () -> Unit,
    onRewindClick: (() -> Unit)? = null,
    onForwardClick: (() -> Unit)? = null,
    onSourceClick: (() -> Unit)? = null,
    onSpeedClick: (() -> Unit)? = null,
    playbackSpeedLabel: String? = null,
    onCastClick: (() -> Unit)? = null,
    onExitFullscreen: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onGestureEnd: () -> Unit,
    audioManager: AudioManager,
    activity: Activity?,
    context: Context,
    playerViewFactory: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .then(
                if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth()
                        .height(292.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(25.dp))
                }
            )
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleControls() })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val screenWidth = size.width
                        val screenHeight = size.height
                        val x = change.position.x
                        val dy = dragAmount.y

                        if (abs(dy) > 5) {
                            if (x < screenWidth / 2) {
                                val currentBrightness = try {
                                    Settings.System.getInt(
                                        context.contentResolver,
                                        Settings.System.SCREEN_BRIGHTNESS
                                    ) / 255f
                                } catch (e: Exception) {
                                    0.5f
                                }
                                val newBrightness = (currentBrightness - dy / screenHeight).coerceIn(0f, 1f)
                                activity?.window?.let { window ->
                                    window.attributes = window.attributes.apply {
                                        screenBrightness = newBrightness
                                    }
                                }
                                onBrightnessChange(newBrightness)
                            } else {
                                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val delta = (-dy / screenHeight * maxVolume).toInt()
                                val newVolume = (currentVolume + delta).coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                onVolumeChange(newVolume)
                            }
                        }
                    },
                    onDragEnd = onGestureEnd
                )
            }
    ) {
        playerViewFactory()

        brightnessOverlay?.let { brightness ->
            GestureOverlay(
                icon = Icons.Default.Brightness6,
                text = "${(brightness * 100).toInt()}%"
            )
        }

        volumeOverlay?.let { volume ->
            GestureOverlay(
                icon = if (volume == 0) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                text = "${(volume * 100 / maxVolume)}%"
            )
        }

        if (!showControls && (playbackUiState.isRecovering || playbackUiState.isFailed)) {
            PlaybackStatusOverlay(
                state = playbackUiState,
                recoveringTitle = recoveringTitle,
                onRetryPlayback = onRetryPlayback
            )
        }

        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
            ) {
                if (isFullscreen) {
                    IconButton(
                        onClick = onExitFullscreen,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                }

                Surface(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(74.dp),
                    color = Color.Black.copy(alpha = 0.54f),
                    contentColor = AppColors.TextPrimary,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.52f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                            onValueChange = { progress -> onSeekTo((progress * duration).toLong()) },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = AppColors.Primary,
                                activeTrackColor = AppColors.Primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                            )
                        )
                        Text(
                            text = formatTime(duration),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isFullscreen) {
                            IconButton(onClick = onToggleFullscreen) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "全屏",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    if (isFullscreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            onRewindClick?.let { rewindClick ->
                                PlayerFullscreenActionButton(
                                    icon = Icons.Default.FastRewind,
                                    label = "快退",
                                    onClick = rewindClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                )
                            }
                            onForwardClick?.let { forwardClick ->
                                PlayerFullscreenActionButton(
                                    icon = Icons.Default.FastForward,
                                    label = "快进",
                                    onClick = forwardClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                )
                            }
                            onSourceClick?.let { sourceClick ->
                                PlayerFullscreenActionButton(
                                    icon = Icons.Default.SwapHoriz,
                                    label = "换源",
                                    onClick = sourceClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                )
                            }
                            if (onSpeedClick != null && playbackSpeedLabel != null) {
                                PlayerFullscreenActionButton(
                                    icon = Icons.Default.Speed,
                                    label = playbackSpeedLabel,
                                    onClick = onSpeedClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                )
                            }
                            onCastClick?.let { castClick ->
                                PlayerFullscreenActionButton(
                                    icon = Icons.Default.Cast,
                                    label = "投屏",
                                    onClick = castClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                )
                            }
                            PlayerFullscreenActionButton(
                                icon = Icons.Default.FullscreenExit,
                                label = "退出",
                                onClick = onToggleFullscreen,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            )
                        }
                    }
                }
            }
        } else if (!isPlaying && !playbackUiState.isFailed && !playbackUiState.isRecovering) {
            Surface(
                onClick = onTogglePlay,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(84.dp),
                color = Color.Black.copy(alpha = 0.52f),
                contentColor = AppColors.TextPrimary,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.PlaybackStatusOverlay(
    state: PlaybackUiState,
    recoveringTitle: String,
    onRetryPlayback: () -> Unit
) {
    Surface(
        onClick = {
            if (state.isFailed) onRetryPlayback()
        },
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp),
        color = Color.Black.copy(alpha = 0.70f),
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (state.isFailed) AppColors.Accent.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.20f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (state.isFailed) "线路不可用" else recoveringTitle,
                color = if (state.isFailed) AppColors.Accent else AppColors.Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = state.message,
                color = Color.White.copy(alpha = 0.84f),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            if (state.isFailed) {
                Text(
                    text = "点击重试",
                    color = AppColors.Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlayerMetaPill(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AppColors.SurfaceAlt)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        color = AppColors.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun PlaybackStatsRow(stats: PlaybackStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlayerStatsCell(
            label = "分辨率",
            value = formatResolution(stats.resolutionWidth, stats.resolutionHeight),
            modifier = Modifier.weight(1f)
        )
        PlayerStatsCell(
            label = "码率",
            value = formatBitrate(stats.videoBitrateBitsPerSecond),
            modifier = Modifier.weight(1f)
        )
        PlayerStatsCell(
            label = "网速",
            value = formatTransferSpeed(stats.networkSpeedBitsPerSecond),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlayerStatsCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(46.dp),
        color = AppColors.SurfaceAlt,
        contentColor = AppColors.TextPrimary,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, AppColors.Divider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = AppColors.TextTertiary,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
            Text(
                text = value,
                color = AppColors.TextPrimary,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }
    }
}

@Composable
private fun PlayerSourceLinkRow(
    url: String,
    onCopyClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        color = AppColors.SurfaceAlt,
        contentColor = AppColors.TextPrimary,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppColors.Divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "当前源地址",
                    color = AppColors.TextTertiary,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = url.ifBlank { "暂无可用地址" },
                    color = AppColors.TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onCopyClick,
                enabled = url.isNotBlank(),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制当前源地址",
                    tint = if (url.isNotBlank()) AppColors.Primary else AppColors.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerUtilityButton(
    icon: ImageVector,
    label: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp)
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = AppColors.SurfaceAlt,
        contentColor = AppColors.TextPrimary,
        shape = shape,
        border = BorderStroke(1.dp, AppColors.Divider)
    ) {
        val horizontalPadding = if (label == null) 0.dp else 8.dp
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            if (label != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun PlayerFullscreenActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.46f),
        contentColor = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false
            )
        }
    }
}

@Composable
private fun GestureOverlay(
    icon: ImageVector,
    text: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = text, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun enterFullscreen(activity: Activity?) {
    activity?.apply {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

private fun exitFullscreen(activity: Activity?) {
    activity?.apply {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = ms / (1000 * 60 * 60)
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun formatResolution(width: Int, height: Int): String {
    return if (width > 0 && height > 0) {
        "${width}x$height"
    } else {
        "待获取"
    }
}

private fun formatBitrate(bitsPerSecond: Long): String {
    if (bitsPerSecond <= 0L) return "待获取"
    return if (bitsPerSecond >= 1_000_000L) {
        val mbps = bitsPerSecond / 1_000_000f
        if (mbps >= 10f) {
            String.format("%.0f Mbps", mbps)
        } else {
            String.format("%.1f Mbps", mbps)
        }
    } else {
        "${(bitsPerSecond / 1_000L).coerceAtLeast(1L)} Kbps"
    }
}

private fun formatTransferSpeed(bitsPerSecond: Long): String {
    if (bitsPerSecond <= 0L) return "待获取"
    val bytesPerSecond = bitsPerSecond / 8f
    return if (bytesPerSecond >= 1_048_576f) {
        val mbps = bytesPerSecond / 1_048_576f
        if (mbps >= 10f) {
            String.format("%.0f MB/s", mbps)
        } else {
            String.format("%.1f MB/s", mbps)
        }
    } else {
        "${(bytesPerSecond / 1024f).toLong().coerceAtLeast(1L)} KB/s"
    }
}

private fun episodeTopBarTitle(title: String, episodeLabel: String): String {
    return when {
        title.isNotBlank() && episodeLabel.isNotBlank() -> "$title · $episodeLabel"
        title.isNotBlank() -> title
        episodeLabel.isNotBlank() -> episodeLabel
        else -> "剧集播放"
    }
}

private fun quickSeekPosition(currentPosition: Long, duration: Long, delta: Long): Long {
    val target = currentPosition + delta
    return if (duration > 0L) {
        target.coerceIn(0L, duration)
    } else {
        target.coerceAtLeast(0L)
    }
}

private fun copyPlayerSourceLink(
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    url: String
) {
    if (url.isBlank()) return
    clipboardManager.setText(AnnotatedString(url))
    Toast.makeText(context, "源地址已复制", Toast.LENGTH_SHORT).show()
}

private fun episodePlayerSubtitle(
    episodeUrl: String,
    currentPosition: Long,
    duration: Long,
    playbackUiState: PlaybackUiState
): String {
    if (playbackUiState.isRecovering || playbackUiState.isFailed) {
        return playbackUiState.message
    }
    return if (duration > 0L) {
        "已播放 ${formatTime(currentPosition)} / ${formatTime(duration)}"
    } else {
        "${playerSourceLabel(episodeUrl)} 线路准备中"
    }
}

private fun livePlayerSubtitle(
    group: String,
    format: String,
    liveUrl: String,
    playbackUiState: PlaybackUiState
): String {
    if (playbackUiState.isRecovering || playbackUiState.isFailed) {
        return playbackUiState.message
    }
    val meta = listOf(group, format, playerSourceLabel(liveUrl))
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    return if (meta.isBlank()) "直播线路已接入" else "$meta · 直播线路已接入"
}

private fun playerSourceLabel(episodeUrl: String): String {
    val parsed = Uri.parse(episodeUrl)
    val hostParts = parsed.host
        ?.split('.')
        ?.filter { it.isNotBlank() }
        .orEmpty()
    val preferredHost = hostParts.firstOrNull { part ->
        part.length > 2 && part !in setOf("www", "m", "v")
    }
    val fallbackHost = hostParts.firstOrNull { it.isNotBlank() }
    if (!preferredHost.isNullOrBlank()) return preferredHost
    if (!fallbackHost.isNullOrBlank()) return fallbackHost
    return when {
        episodeUrl.contains("m3u8", ignoreCase = true) -> "m3u8"
        episodeUrl.contains("mp4", ignoreCase = true) -> "mp4"
        else -> "线路"
    }
}
