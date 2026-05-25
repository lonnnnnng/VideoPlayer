package com.zy.player.ui.screens.searchresult

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zy.player.data.remote.VodItem
import com.zy.player.data.repository.SiteRepository
import com.zy.player.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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

sealed class SearchResultUiState {
    object Loading : SearchResultUiState()
    data class Success(val vodList: List<SearchResultItem>, val hasMore: Boolean) : SearchResultUiState()
    data class Error(val message: String) : SearchResultUiState()
    object Empty : SearchResultUiState()
}

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val vodRepository: VodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "SearchResultViewModel"
    }

    val keyword: String = Uri.decode(savedStateHandle.get<String>("keyword") ?: "")

    private val _uiState = MutableStateFlow<SearchResultUiState>(SearchResultUiState.Loading)
    val uiState: StateFlow<SearchResultUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val enabledSites = mutableListOf<SearchSite>()

    init {
        loadEnabledSites()
    }

    private fun loadEnabledSites() {
        viewModelScope.launch {
            val sites = siteRepository.getEnabledSites()
            enabledSites.clear()
            enabledSites.addAll(sites.map { SearchSite(it.id, it.name, it.apiUrl) })
            Log.d(TAG, "loadEnabledSites - loaded ${enabledSites.size} enabled sites")
            search()
        }
    }

    fun search() {
        currentPage = 1
        searchInternal(isRefresh = true)
    }

    fun loadMore() {
        currentPage++
        searchInternal(isRefresh = false)
    }

    private fun searchInternal(isRefresh: Boolean) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = SearchResultUiState.Loading
            }

            val results = mutableListOf<SearchResultItem>()
            var hasError = false
            var errorMessage = ""

            for (site in enabledSites) {
                vodRepository.getVodList(site.apiUrl, currentPage, keyword = keyword).fold(
                    onSuccess = { response ->
                        val siteResults = response.list.orEmpty()
                        Log.d(
                            TAG,
                            "searchInternal - site=${site.name}, page=$currentPage, keyword=$keyword, count=${siteResults.size}"
                        )
                        siteResults.forEach { vod ->
                            results.add(
                                SearchResultItem(
                                    siteId = site.id,
                                    siteName = site.name,
                                    vod = vod
                                )
                            )
                        }
                    },
                    onFailure = { error ->
                        hasError = true
                        errorMessage = error.message ?: "搜索失败"
                        Log.w(TAG, "searchInternal - site=${site.name} failed: ${error.message}")
                    }
                )
            }

            when {
                results.isEmpty() && hasError -> {
                    _uiState.value = SearchResultUiState.Error(errorMessage)
                }
                results.isEmpty() -> {
                    _uiState.value = SearchResultUiState.Empty
                }
                else -> {
                    val existingList = if (isRefresh) emptyList() else {
                        (_uiState.value as? SearchResultUiState.Success)?.vodList ?: emptyList()
                    }
                    _uiState.value = SearchResultUiState.Success(
                        vodList = existingList + results,
                        hasMore = results.size >= 20
                    )
                }
            }
        }
    }
}
