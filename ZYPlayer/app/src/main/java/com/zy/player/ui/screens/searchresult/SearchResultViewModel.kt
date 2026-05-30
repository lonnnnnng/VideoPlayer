package com.zy.player.ui.screens.searchresult

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zy.player.data.remote.VodItem
import com.zy.player.data.repository.NetworkSettingsRepository
import com.zy.player.data.repository.SiteRepository
import com.zy.player.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchResultItem(
    val siteId: Long,
    val siteName: String,
    val vod: VodItem
)

private data class SearchSite(
    val id: Long,
    val name: String,
    val apiUrl: String
)

data class SearchSummary(
    val totalSources: Int = 0,
    val completedSources: Int = 0,
    val elapsedSeconds: Double = 0.0,
    val validResultCount: Int = 0,
    val isSearching: Boolean = false
) {
    val text: String
        get() = "本次共调用 ${totalSources} 个源，已完成 ${completedSources} 个，搜索 %.1f 秒，共获得 ${validResultCount} 条有效相关结果"
            .format(elapsedSeconds)
}

sealed class SearchResultUiState {
    data class Success(
        val vodList: List<SearchResultItem> = emptyList(),
        val summary: SearchSummary = SearchSummary()
    ) : SearchResultUiState()

    data class Error(
        val message: String,
        val summary: SearchSummary = SearchSummary()
    ) : SearchResultUiState()
}

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val vodRepository: VodRepository,
    private val networkSettingsRepository: NetworkSettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val keyword: String = Uri.decode(savedStateHandle.get<String>("keyword") ?: "")

    private val _uiState = MutableStateFlow<SearchResultUiState>(
        SearchResultUiState.Success(summary = SearchSummary(isSearching = true))
    )
    val uiState: StateFlow<SearchResultUiState> = _uiState.asStateFlow()

    init {
        search()
    }

    fun search() {
        viewModelScope.launch {
            val sites = siteRepository.getEnabledSites()
                .map { SearchSite(it.id, it.name, it.apiUrl) }
            val startedAt = System.currentTimeMillis()
            val results = ConcurrentHashMap<String, SearchResultItem>()
            val completedCount = AtomicInteger(0)

            publishResults(
                startedAt = startedAt,
                totalSources = sites.size,
                completedSources = 0,
                results = emptyList(),
                isSearching = sites.isNotEmpty()
            )

            if (sites.isEmpty()) {
                _uiState.value = SearchResultUiState.Error(
                    message = "没有启用的视频源",
                    summary = SearchSummary()
                )
                return@launch
            }

            coroutineScope {
                sites.map { site ->
                    async(Dispatchers.IO) {
                        vodRepository.getVodList(
                            baseUrl = site.apiUrl,
                            page = 1,
                            keyword = keyword,
                            timeoutMs = networkSettingsRepository.currentSettings().videoSourceTimeoutMs,
                            forceRefresh = true
                        ).fold(
                            onSuccess = { response ->
                                response.list.orEmpty()
                                    .filter { it.vod_name.contains(keyword, ignoreCase = true) }
                                    .forEach { vod ->
                                        val key = "${site.id}:${vod.vod_id}"
                                        results[key] = SearchResultItem(
                                            siteId = site.id,
                                            siteName = site.name,
                                            vod = vod
                                        )
                                    }
                            },
                            onFailure = { error ->
                                Log.w(TAG, "search - site=${site.name} failed: ${error.message}")
                            }
                        )

                        val completed = completedCount.incrementAndGet()
                        val finished = completed >= sites.size
                        publishResults(
                            startedAt = startedAt,
                            totalSources = sites.size,
                            completedSources = completed,
                            results = sortSearchResults(results.values, finalized = finished),
                            isSearching = !finished
                        )
                    }
                }.awaitAll()
            }
        }
    }

    private fun publishResults(
        startedAt: Long,
        totalSources: Int,
        completedSources: Int,
        results: List<SearchResultItem>,
        isSearching: Boolean
    ) {
        val elapsed = (System.currentTimeMillis() - startedAt) / 1000.0
        _uiState.value = SearchResultUiState.Success(
            vodList = results,
            summary = SearchSummary(
                totalSources = totalSources,
                completedSources = completedSources,
                elapsedSeconds = elapsed,
                validResultCount = results.size,
                isSearching = isSearching
            )
        )
    }

    private fun sortSearchResults(
        results: Collection<SearchResultItem>,
        finalized: Boolean
    ): List<SearchResultItem> {
        return if (finalized) {
            results.sortedWith(
                compareByDescending<SearchResultItem> { it.vod.updateSortValue() }
                    .thenBy { it.vod.vod_name }
                    .thenBy { it.siteName }
            )
        } else {
            results.sortedBy { it.vod.vod_name }
        }
    }

    private fun VodItem.updateSortValue(): Long {
        return parseVodUpdateTime(vod_time)
            ?: parseVodUpdateTime(vod_time_add)
            ?: Long.MIN_VALUE
    }

    private fun parseVodUpdateTime(value: String?): Long? {
        val normalized = value?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        if (normalized.all { it.isDigit() }) {
            val raw = normalized.toLongOrNull() ?: return null
            return if (normalized.length >= 13) raw else raw * 1_000L
        }

        val normalizedDate = normalized
            .replace('T', ' ')
            .substringBefore('+')
            .substringBefore('Z')
            .trim()
        return UPDATE_TIME_PATTERNS.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.getDefault()).parse(normalizedDate)?.time
            }.getOrNull()
        }
    }

    companion object {
        private const val TAG = "SearchResultViewModel"
        private val UPDATE_TIME_PATTERNS = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd",
            "yyyy/MM/dd"
        )
    }
}
