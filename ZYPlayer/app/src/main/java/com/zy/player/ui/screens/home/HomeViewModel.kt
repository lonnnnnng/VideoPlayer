package com.zy.player.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zy.player.data.local.entity.VideoSiteEntity
import com.zy.player.data.remote.VodItem
import com.zy.player.data.repository.SiteRepository
import com.zy.player.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeVodSource(
    val siteId: Long,
    val siteName: String,
    val vod: VodItem
) {
    val key: String = "$siteId:${vod.vod_id}"
}

data class HomeVodItem(
    val groupKey: String,
    val sources: List<HomeVodSource>
) {
    val primary: HomeVodSource = sources.first()
    val siteId: Long = primary.siteId
    val siteName: String = primary.siteName
    val vod: VodItem = primary.vod
    val sourceCount: Int = sources.map { it.siteId }.distinct().size
    val sourceNames: List<String> = sources.map { it.siteName }.distinct()
    val key: String = groupKey
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val vodList: List<HomeVodItem>,
        val hasMore: Boolean,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val isAggregating: Boolean = false,
        val warningMessage: String? = null
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    object Empty : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val vodRepository: VodRepository
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
        private const val TITLE_AGGREGATE_IGNORED_CHARS = ":：·・,，.。!！?？_-—/\\()[]【】{}《》<>"
    }

    private data class SitePageResult(
        val site: VideoSiteEntity,
        val page: Int,
        val items: List<HomeVodSource>,
        val hasMore: Boolean,
        val error: Throwable?
    )

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var isLoadingVodList = false
    private var enabledSitesSnapshot: List<VideoSiteEntity> = emptyList()
    private var enabledSiteSignature: String? = null
    private val nextPageBySite = mutableMapOf<Long, Int>()
    private val siteHasMoreById = mutableMapOf<Long, Boolean>()

    init {
        viewModelScope.launch {
            siteRepository.observeAllSites().collectLatest { siteList ->
                val enabledSites = siteList.filter { it.enabled }
                val signature = enabledSites.joinToString("|") {
                    "${it.id}:${it.name}:${it.apiUrl}:${it.sortOrder}"
                }
                if (signature != enabledSiteSignature) {
                    enabledSiteSignature = signature
                    enabledSitesSnapshot = enabledSites
                    resetPaging()
                    loadVodList(isRefresh = true)
                }
            }
        }
    }

    fun loadVodList(isRefresh: Boolean = false) {
        val previousState = _uiState.value
        if (isLoadingVodList) return
        if (!isRefresh) {
            val successState = previousState as? HomeUiState.Success ?: return
            if (!successState.hasMore || successState.isLoadingMore || successState.isRefreshing) return
        }

        isLoadingVodList = true
        viewModelScope.launch {
            val enabledSites = enabledSitesSnapshot.ifEmpty {
                siteRepository.getEnabledSites().also { enabledSitesSnapshot = it }
            }

            if (enabledSites.isEmpty()) {
                _uiState.value = HomeUiState.Empty
                isLoadingVodList = false
                return@launch
            }

            if (isRefresh) {
                resetPaging()
                _uiState.value = when (previousState) {
                    is HomeUiState.Success -> previousState.copy(
                        isRefreshing = true,
                        isLoadingMore = false,
                        warningMessage = null
                    )
                    else -> HomeUiState.Loading
                }
            } else {
                val successState = previousState as? HomeUiState.Success
                if (successState != null) {
                    _uiState.value = successState.copy(isLoadingMore = true)
                }
            }

            try {
                val sitesToLoad = enabledSites.filter { site ->
                    isRefresh || siteHasMoreById[site.id] != false
                }

                if (sitesToLoad.isEmpty()) {
                    _uiState.value = (previousState as? HomeUiState.Success)?.copy(
                        hasMore = false,
                        isRefreshing = false,
                        isLoadingMore = false
                    ) ?: HomeUiState.Empty
                    return@launch
                }

                loadSitePagesProgressively(
                    enabledSites = enabledSites,
                    sitesToLoad = sitesToLoad,
                    isRefresh = isRefresh,
                    previousState = previousState
                )
            } finally {
                isLoadingVodList = false
            }
        }
    }

    fun loadMore() {
        loadVodList(isRefresh = false)
    }

    fun refresh() {
        loadVodList(isRefresh = true)
    }

    private suspend fun loadSitePagesProgressively(
        enabledSites: List<VideoSiteEntity>,
        sitesToLoad: List<VideoSiteEntity>,
        isRefresh: Boolean,
        previousState: HomeUiState
    ) = coroutineScope {
        val existingSources = if (isRefresh) {
            emptyList()
        } else {
            (previousState as? HomeUiState.Success)?.vodList
                .orEmpty()
                .flatMap { it.sources }
        }.toMutableList()
        val itemsBySite = linkedMapOf<Long, List<HomeVodSource>>()
        var loadedCount = 0
        var failedCount = 0
        var firstError: Throwable? = null
        val resultChannel = Channel<SitePageResult>(Channel.UNLIMITED)

        sitesToLoad.forEach { site ->
            launch {
                resultChannel.send(
                    loadSitePage(
                        site = site,
                        page = if (isRefresh) 1 else nextPageBySite[site.id] ?: 1,
                        forceRefresh = isRefresh
                    )
                )
            }
        }

        repeat(sitesToLoad.size) {
            val result = resultChannel.receive()
            loadedCount += 1
            if (result.error == null) {
                nextPageBySite[result.site.id] = result.page + 1
                siteHasMoreById[result.site.id] = result.hasMore
                itemsBySite[result.site.id] = result.items
            } else {
                siteHasMoreById[result.site.id] = false
                failedCount += 1
                firstError = firstError ?: result.error
            }

            val newItems = interleaveSiteItems(
                enabledSites = enabledSites,
                itemsBySite = itemsBySite
            )
            val combinedList = aggregateHomeVodItems(
                sources = existingSources + newItems,
                enabledSites = enabledSites
            )
            val isStillAggregating = loadedCount < sitesToLoad.size
            val hasMore = enabledSites.any { siteHasMoreById[it.id] == true } || isStillAggregating
            val warningMessage = buildWarningMessage(
                failedCount = failedCount,
                isStillAggregating = isStillAggregating
            )

            _uiState.value = when {
                combinedList.isNotEmpty() -> HomeUiState.Success(
                    vodList = combinedList,
                    hasMore = hasMore,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isAggregating = isStillAggregating,
                    warningMessage = warningMessage
                )
                isRefresh && previousState is HomeUiState.Success && failedCount > 0 -> previousState.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                    isAggregating = isStillAggregating,
                    warningMessage = "刷新失败，已保留当前列表"
                )
                !isStillAggregating && failedCount > 0 -> HomeUiState.Error(
                    firstError?.message ?: "所有视频源加载失败"
                )
                !isStillAggregating -> HomeUiState.Empty
                else -> _uiState.value
            }
        }
        resultChannel.close()
    }

    private suspend fun loadSitePage(
        site: VideoSiteEntity,
        page: Int,
        forceRefresh: Boolean
    ): SitePageResult {
        return vodRepository.getVodList(
            baseUrl = site.apiUrl,
            page = page,
            forceRefresh = forceRefresh
        ).fold(
            onSuccess = { response ->
                val items = response.list.orEmpty().map { vod ->
                    HomeVodSource(
                        siteId = site.id,
                        siteName = site.name,
                        vod = vod
                    )
                }
                val responsePage = response.page ?: page
                val hasMore = items.isNotEmpty() && (
                    response.pagecount?.let { responsePage < it }
                        ?: (items.size >= PAGE_SIZE)
                    )
                SitePageResult(
                    site = site,
                    page = page,
                    items = items,
                    hasMore = hasMore,
                    error = null
                )
            },
            onFailure = { error ->
                SitePageResult(
                    site = site,
                    page = page,
                    items = emptyList(),
                    hasMore = false,
                    error = error
                )
            }
        )
    }

    private fun buildWarningMessage(
        failedCount: Int,
        isStillAggregating: Boolean
    ): String? {
        return when {
            failedCount > 0 && isStillAggregating -> "${failedCount} 个视频源加载失败，其他源仍在聚合"
            failedCount > 0 -> "${failedCount} 个视频源加载失败，已显示可用内容"
            isStillAggregating -> "正在聚合其他视频源"
            else -> null
        }
    }

    private fun interleaveSiteItems(
        enabledSites: List<VideoSiteEntity>,
        itemsBySite: Map<Long, List<HomeVodSource>>
    ): List<HomeVodSource> {
        val orderedSiteItems = enabledSites.map { site ->
            itemsBySite[site.id].orEmpty()
        }
        val maxSize = orderedSiteItems.maxOfOrNull { it.size } ?: 0
        return buildList {
            for (index in 0 until maxSize) {
                orderedSiteItems.forEach { items ->
                    items.getOrNull(index)?.let { add(it) }
                }
            }
        }
    }

    private fun aggregateHomeVodItems(
        sources: List<HomeVodSource>,
        enabledSites: List<VideoSiteEntity>
    ): List<HomeVodItem> {
        val siteRank = enabledSites.mapIndexed { index, site -> site.id to index }.toMap()
        val grouped = linkedMapOf<String, MutableList<HomeVodSource>>()

        sources.forEach { source ->
            val groupKey = aggregateKey(source.vod)
            if (groupKey.isBlank()) {
                grouped.getOrPut(source.key) { mutableListOf() } += source
            } else {
                grouped.getOrPut(groupKey) { mutableListOf() } += source
            }
        }

        return grouped.map { (groupKey, groupSources) ->
            val uniqueSources = groupSources
                .distinctBy { it.siteId }
                .sortedWith(
                    compareBy<HomeVodSource> { siteRank[it.siteId] ?: Int.MAX_VALUE }
                        .thenBy { it.siteName }
                )
            HomeVodItem(groupKey = groupKey, sources = uniqueSources)
        }
    }

    private fun aggregateKey(vod: VodItem): String {
        return vod.vod_name
            .lowercase()
            .filterNot { char ->
                char.isWhitespace() || char in TITLE_AGGREGATE_IGNORED_CHARS
            }
    }

    private fun resetPaging() {
        nextPageBySite.clear()
        siteHasMoreById.clear()
    }
}
