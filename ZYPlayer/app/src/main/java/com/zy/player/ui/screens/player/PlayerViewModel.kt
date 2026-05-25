package com.zy.player.ui.screens.player

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.zy.player.data.repository.HistoryRepository
import com.zy.player.data.repository.SiteRepository
import com.zy.player.data.repository.VodRepository
import com.zy.player.domain.model.EpisodeGroup
import com.zy.player.domain.model.EpisodeItem
import com.zy.player.domain.parser.VodPlayUrlParser
import com.zy.player.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

data class PlaybackUiState(
    val sourceName: String = "线路",
    val message: String = "正在检测线路",
    val isRecovering: Boolean = true,
    val isFailed: Boolean = false
)

data class PlayerSourceOption(
    val key: String,
    val sourceName: String,
    val episodeLabel: String,
    val isCurrent: Boolean
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val historyRepository: HistoryRepository,
    private val siteRepository: SiteRepository,
    private val vodRepository: VodRepository,
    private val okHttpClient: OkHttpClient,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "PlayerViewModel"
    }

    private val siteId: Long = savedStateHandle.get<Long>("siteId") ?: 0L
    private val vodId: String = savedStateHandle.get<String>("vodId") ?: ""
    val episodeUrl: String = savedStateHandle.get<String>("episodeUrl") ?: ""
    private val title: String = savedStateHandle.get<String>("title").orEmpty()
    private val episodeLabel: String = savedStateHandle.get<String>("episodeLabel").orEmpty()

    private data class PlaybackCandidate(
        val siteId: Long,
        val vodId: String,
        val url: String,
        val sourceName: String,
        val episodeLabel: String
    ) {
        val key: String = url
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _activeEpisodeUrl = MutableStateFlow(episodeUrl)
    val activeEpisodeUrl: StateFlow<String> = _activeEpisodeUrl.asStateFlow()

    private val _playbackUiState = MutableStateFlow(PlaybackUiState())
    val playbackUiState: StateFlow<PlaybackUiState> = _playbackUiState.asStateFlow()

    private val _sourceOptions = MutableStateFlow<List<PlayerSourceOption>>(emptyList())
    val sourceOptions: StateFlow<List<PlayerSourceOption>> = _sourceOptions.asStateFlow()

    private var progressUpdateJob: Job? = null
    private val playbackCandidates = mutableListOf<PlaybackCandidate>()
    private var currentCandidateIndex = 0
    private var fallbackCandidatesLoaded = false
    private var isRecoveringPlayback = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "onIsPlayingChanged - isPlaying=$isPlaying")
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateStr = when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN"
            }
            Log.d(TAG, "onPlaybackStateChanged - state=$stateStr")
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    val sourceName = playbackCandidates.getOrNull(currentCandidateIndex)?.sourceName ?: "线路"
                    _playbackUiState.value = PlaybackUiState(
                        sourceName = sourceName,
                        message = "$sourceName 正在缓冲",
                        isRecovering = false
                    )
                }
                Player.STATE_READY -> {
                    _duration.value = playerManager.getDuration().coerceAtLeast(0L)
                    val sourceName = playbackCandidates.getOrNull(currentCandidateIndex)?.sourceName ?: "线路"
                    _playbackUiState.value = PlaybackUiState(
                        sourceName = sourceName,
                        message = "$sourceName 已接入",
                        isRecovering = false
                    )
                    Log.d(TAG, "onPlaybackStateChanged - duration=${_duration.value}ms")
                }
                Player.STATE_ENDED -> {
                    val sourceName = playbackCandidates.getOrNull(currentCandidateIndex)?.sourceName ?: "线路"
                    _playbackUiState.value = PlaybackUiState(
                        sourceName = sourceName,
                        message = "播放完成",
                        isRecovering = false
                    )
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "onPlayerError - errorCode=${error.errorCode}, message=${error.message}", error)
            if (vodId == "live") {
                val sourceName = playbackCandidates.getOrNull(currentCandidateIndex)?.sourceName ?: "直播线路"
                _isPlaying.value = false
                stopProgressUpdates()
                _playbackUiState.value = PlaybackUiState(
                    sourceName = sourceName,
                    message = "$sourceName 连接失败：${error.message ?: "网络不可达"}",
                    isRecovering = false,
                    isFailed = true
                )
                return
            }
            recoverFromPlaybackError(error)
        }
    }

    init {
        Log.d(TAG, "init - siteId=$siteId, vodId=$vodId")
        Log.d(TAG, "init - episodeUrl=$episodeUrl")
        playerManager.addListener(playerListener)
        if (episodeUrl.isNotBlank()) {
            playbackCandidates += PlaybackCandidate(
                siteId = siteId,
                vodId = vodId,
                url = episodeUrl,
                sourceName = sourceNameFromUrl(episodeUrl),
                episodeLabel = episodeLabel
            )
            publishSourceOptions()
            viewModelScope.launch {
                startInitialPlayback()
            }
        } else {
            Log.w(TAG, "init - episodeUrl is blank, not starting playback")
            _playbackUiState.value = PlaybackUiState(
                message = "没有可播放地址",
                isRecovering = false,
                isFailed = true
            )
        }
    }

    fun getPlayer() = playerManager.getPlayer()

    fun togglePlayPause() {
        if (_isPlaying.value) {
            Log.d(TAG, "togglePlayPause - Pausing")
            playerManager.pause()
        } else {
            Log.d(TAG, "togglePlayPause - Resuming")
            playerManager.resume()
        }
    }

    fun seekTo(positionMs: Long) {
        Log.d(TAG, "seekTo - position=${positionMs}ms")
        playerManager.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun setSpeed(speed: Float) {
        Log.d(TAG, "setSpeed - speed=${speed}x")
        playerManager.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
    }

    fun retryPlayback() {
        viewModelScope.launch {
            if (playbackCandidates.isEmpty()) {
                startInitialPlayback()
            } else {
                playCandidate(currentCandidateIndex.coerceIn(0, playbackCandidates.lastIndex))
            }
        }
    }

    fun loadSourceOptions() {
        if (vodId == "live") {
            publishSourceOptions()
            return
        }
        viewModelScope.launch {
            ensureFallbackCandidatesLoaded()
        }
    }

    fun switchToSource(sourceKey: String) {
        viewModelScope.launch {
            ensureFallbackCandidatesLoaded()
            val index = playbackCandidates.indexOfFirst { it.key == sourceKey }
            if (index >= 0) {
                val candidate = playbackCandidates[index]
                Log.d(TAG, "switchToSource - index=$index, source=${candidate.sourceName}")
                if (index == currentCandidateIndex || vodId == "live" || isPlayableCandidate(candidate)) {
                    playCandidate(index)
                } else {
                    val currentSource = playbackCandidates.getOrNull(currentCandidateIndex)?.sourceName ?: "当前线路"
                    _playbackUiState.value = PlaybackUiState(
                        sourceName = currentSource,
                        message = "${candidate.sourceName} 暂不可用，已保持 $currentSource 播放",
                        isRecovering = false,
                        isFailed = false
                    )
                    Log.w(TAG, "switchToSource - candidate not playable, keep current source=$currentSource")
                }
            } else {
                Log.w(TAG, "switchToSource - source not found: $sourceKey")
            }
        }
    }

    private suspend fun startInitialPlayback() {
        val candidate = playbackCandidates.firstOrNull()
        if (candidate == null) {
            _playbackUiState.value = PlaybackUiState(
                message = "没有可播放地址",
                isRecovering = false,
                isFailed = true
            )
            return
        }

        _playbackUiState.value = PlaybackUiState(
            sourceName = candidate.sourceName,
            message = "正在检测 ${candidate.sourceName}",
            isRecovering = true
        )

        val playable = vodId == "live" || isPlayableCandidate(candidate)
        if (playable) {
            playCandidate(0)
            return
        }

        Log.w(TAG, "startInitialPlayback - initial URL is not playable, url=${candidate.url}")
        val nextIndex = findNextPlayableCandidate()
        if (nextIndex != null) {
            playCandidate(nextIndex)
        } else {
            _playbackUiState.value = PlaybackUiState(
                sourceName = candidate.sourceName,
                message = "当前影片所有已知线路均不可播放",
                isRecovering = false,
                isFailed = true
            )
        }
    }

    private fun playCandidate(index: Int) {
        val candidate = playbackCandidates.getOrNull(index) ?: return
        currentCandidateIndex = index
        _activeEpisodeUrl.value = candidate.url
        _currentPosition.value = 0L
        _duration.value = 0L
        publishSourceOptions()
        _playbackUiState.value = PlaybackUiState(
            sourceName = candidate.sourceName,
            message = "正在连接 ${candidate.sourceName}",
            isRecovering = index > 0
        )
        Log.d(TAG, "playCandidate - index=$index, source=${candidate.sourceName}, label=${candidate.episodeLabel}, url=${candidate.url}")
        playerManager.play(candidate.url)
    }

    private fun recoverFromPlaybackError(error: PlaybackException) {
        if (isRecoveringPlayback) return
        isRecoveringPlayback = true
        viewModelScope.launch {
            val failedSource = playbackCandidates.getOrNull(currentCandidateIndex)?.sourceName ?: "当前线路"
            _playbackUiState.value = PlaybackUiState(
                sourceName = failedSource,
                message = "$failedSource 播放失败，正在切换备用线路",
                isRecovering = true
            )

            val nextIndex = findNextPlayableCandidate()
            if (nextIndex != null) {
                playCandidate(nextIndex)
            } else {
                _playbackUiState.value = PlaybackUiState(
                    sourceName = failedSource,
                    message = "所有已知线路均不可播放：${error.message ?: "资源失效"}",
                    isRecovering = false,
                    isFailed = true
                )
            }
            isRecoveringPlayback = false
        }
    }

    private suspend fun findNextPlayableCandidate(): Int? {
        var nextIndex = currentCandidateIndex + 1
        while (nextIndex < playbackCandidates.size) {
            if (isPlayableCandidate(playbackCandidates[nextIndex])) return nextIndex
            nextIndex++
        }

        if (!fallbackCandidatesLoaded) {
            fallbackCandidatesLoaded = true
            mergePlaybackCandidates(loadFallbackCandidates())
        }

        nextIndex = currentCandidateIndex + 1
        while (nextIndex < playbackCandidates.size) {
            if (isPlayableCandidate(playbackCandidates[nextIndex])) return nextIndex
            nextIndex++
        }
        return null
    }

    private suspend fun ensureFallbackCandidatesLoaded() {
        if (!fallbackCandidatesLoaded) {
            fallbackCandidatesLoaded = true
            mergePlaybackCandidates(loadFallbackCandidates())
        }
        publishSourceOptions()
    }

    private suspend fun loadFallbackCandidates(): List<PlaybackCandidate> {
        if (title.isBlank() || episodeLabel.isBlank()) return emptyList()

        _playbackUiState.value = PlaybackUiState(
            message = "正在查找《$title》的备用线路",
            isRecovering = true
        )

        val candidates = mutableListOf<PlaybackCandidate>()
        val enabledSites = siteRepository.getEnabledSites()
        enabledSites.forEach { site ->
            val detailItems = if (site.id == siteId && vodId.isNotBlank()) {
                vodRepository.getVodDetail(site.apiUrl, vodId).getOrNull()
                    ?.list
                    .orEmpty()
            } else {
                val searchResults = vodRepository.getVodList(
                    baseUrl = site.apiUrl,
                    page = 1,
                    keyword = title
                ).getOrNull()?.list.orEmpty()
                val matched = searchResults.firstOrNull {
                    normalizeTitle(it.vod_name) == normalizeTitle(title)
                } ?: searchResults.firstOrNull {
                    normalizeTitle(it.vod_name).contains(normalizeTitle(title)) ||
                        normalizeTitle(title).contains(normalizeTitle(it.vod_name))
                } ?: searchResults.firstOrNull()
                matched?.let { item ->
                    vodRepository.getVodDetail(site.apiUrl, item.vod_id.toString()).getOrNull()?.list.orEmpty()
                }.orEmpty()
            }

            detailItems.firstOrNull()?.let { vod ->
                val groups = VodPlayUrlParser.parseGroups(vod.vod_play_from, vod.vod_play_url)
                candidates += buildCandidatesForEpisode(
                    siteId = site.id,
                    vodId = vod.vod_id.toString(),
                    sourceName = site.name,
                    groups = groups,
                    label = episodeLabel
                )
            }
        }

        Log.d(TAG, "loadFallbackCandidates - loaded ${candidates.size} candidates")
        return candidates.distinctBy { it.url }
    }

    private fun buildCandidatesForEpisode(
        siteId: Long,
        vodId: String,
        sourceName: String,
        groups: List<EpisodeGroup>,
        label: String
    ): List<PlaybackCandidate> {
        val exactMatches = groups.flatMap { group ->
            group.episodes
                .filter { labelsMatch(it.label, label) }
                .map { group to it }
        }
        val fallbackMatches = if (exactMatches.isEmpty()) {
            groups.mapNotNull { group -> group.episodes.firstOrNull()?.let { group to it } }
        } else {
            exactMatches
        }

        return fallbackMatches
            .sortedWith(
                compareByDescending<Pair<EpisodeGroup, EpisodeItem>> {
                    it.second.url.contains(".m3u8", ignoreCase = true)
                }.thenByDescending {
                    it.first.name.contains("m3u8", ignoreCase = true) ||
                        it.first.name.contains("hls", ignoreCase = true)
                }
            )
            .map { (group, episode) ->
                PlaybackCandidate(
                    siteId = siteId,
                    vodId = vodId,
                    url = episode.url,
                    sourceName = "$sourceName/${group.name}",
                    episodeLabel = episode.label
                )
            }
    }

    private suspend fun isPlayableCandidate(candidate: PlaybackCandidate): Boolean = withContext(Dispatchers.IO) {
        val url = candidate.url
        if (!url.startsWith("http://") && !url.startsWith("https://")) return@withContext false

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
            .header("Referer", "https://www.baidu.com/")
            .header("Range", "bytes=0-4095")
            .get()
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                val bodySample = response.peekBody(4096).string()
                val contentType = response.header("Content-Type").orEmpty().lowercase()
                val playable = response.isSuccessful && (
                    bodySample.contains("#EXTM3U", ignoreCase = true) ||
                        contentType.contains("mpegurl") ||
                        contentType.contains("video")
                    )
                Log.d(
                    TAG,
                    "isPlayableCandidate - source=${candidate.sourceName}, code=${response.code}, type=$contentType, playable=$playable, url=$url"
                )
                playable
            }
        }.getOrElse { error ->
            Log.w(TAG, "isPlayableCandidate - probe failed: ${error.message}, url=$url")
            false
        }
    }

    private fun mergePlaybackCandidates(candidates: List<PlaybackCandidate>) {
        candidates.forEach { candidate ->
            val existingIndex = playbackCandidates.indexOfFirst { it.url == candidate.url }
            if (existingIndex >= 0) {
                playbackCandidates[existingIndex] = candidate
            } else {
                playbackCandidates += candidate
            }
        }
        publishSourceOptions()
    }

    private fun publishSourceOptions() {
        _sourceOptions.value = playbackCandidates.mapIndexed { index, candidate ->
            PlayerSourceOption(
                key = candidate.key,
                sourceName = candidate.sourceName,
                episodeLabel = candidate.episodeLabel,
                isCurrent = index == currentCandidateIndex
            )
        }
    }

    private fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                _currentPosition.value = playerManager.getCurrentPosition()
                delay(500)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
    }

    fun savePlaybackPosition(vodName: String, vodPic: String, episodeLabel: String) {
        viewModelScope.launch {
            Log.d(TAG, "savePlaybackPosition - vodName=$vodName, position=${_currentPosition.value}ms, duration=${_duration.value}ms")
            val activeCandidate = playbackCandidates.getOrNull(currentCandidateIndex)
            historyRepository.recordPlayback(
                siteId = activeCandidate?.siteId ?: siteId,
                vodId = activeCandidate?.vodId ?: vodId,
                vodName = vodName,
                vodPic = vodPic,
                episodeLabel = episodeLabel,
                episodeUrl = _activeEpisodeUrl.value,
                positionMs = _currentPosition.value,
                durationMs = _duration.value
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared - Cleaning up")
        stopProgressUpdates()
        playerManager.removeListener(playerListener)
    }

    private fun labelsMatch(left: String, right: String): Boolean {
        if (left == right) return true
        val leftNumber = left.filter { it.isDigit() }.trimStart('0')
        val rightNumber = right.filter { it.isDigit() }.trimStart('0')
        return leftNumber.isNotBlank() && leftNumber == rightNumber
    }

    private fun normalizeTitle(value: String): String {
        return value
            .lowercase()
            .replace(" ", "")
            .replace("　", "")
            .replace(":", "")
            .replace("：", "")
    }

    private fun sourceNameFromUrl(url: String): String {
        val host = android.net.Uri.parse(url).host.orEmpty()
        return host
            .split('.')
            .firstOrNull { it.length > 2 && it !in setOf("www", "vip", "vod") }
            ?: "当前线路"
    }
}
