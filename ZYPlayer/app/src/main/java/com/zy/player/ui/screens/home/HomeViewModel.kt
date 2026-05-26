package com.zy.player.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zy.player.data.remote.VodItem
import com.zy.player.data.repository.SiteRepository
import com.zy.player.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val vodList: List<VodItem>,
        val hasMore: Boolean,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    object Empty : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val vodRepository: VodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _currentSiteId = MutableStateFlow<Long?>(null)
    val currentSiteId: StateFlow<Long?> = _currentSiteId.asStateFlow()

    private val _categories = MutableStateFlow<List<Pair<Int, String>>>(emptyList())
    val categories: StateFlow<List<Pair<Int, String>>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private var currentPage = 1
    private var currentBaseUrl: String? = null
    private var isLoadingVodList = false

    val sites = siteRepository.observeAllSites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            sites.collectLatest { siteList ->
                val enabledSites = siteList.filter { it.enabled }
                if (enabledSites.isNotEmpty() && _currentSiteId.value == null) {
                    selectSite(enabledSites.first().id)
                }
            }
        }
    }

    fun selectSite(siteId: Long) {
        viewModelScope.launch {
            _currentSiteId.value = siteId
            val site = sites.value.find { it.id == siteId }
            if (site != null) {
                currentBaseUrl = site.apiUrl
                _categories.value = emptyList()
                _selectedCategoryId.value = null
                currentPage = 1
                loadCategories()
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val baseUrl = currentBaseUrl ?: return@launch
            vodRepository.getCategories(baseUrl).fold(
                onSuccess = { response ->
                    val cats = response.`class`?.map { it.type_id to it.type_name } ?: emptyList()
                    _categories.value = listOf(0 to "全部") + cats
                    selectCategory(0)
                },
                onFailure = {
                    _categories.value = listOf(0 to "全部")
                    selectCategory(0)
                }
            )
        }
    }

    fun selectCategory(categoryId: Int) {
        _selectedCategoryId.value = categoryId
        currentPage = 1
        loadVodList(isRefresh = true)
    }

    fun loadVodList(isRefresh: Boolean = false) {
        val baseUrl = currentBaseUrl ?: return
        val previousState = _uiState.value
        val previousPage = currentPage
        val pageToLoad = if (isRefresh) 1 else currentPage
        val typeId = _selectedCategoryId.value?.takeIf { it != 0 }

        if (isLoadingVodList) return
        if (!isRefresh) {
            val successState = previousState as? HomeUiState.Success ?: return
            if (!successState.hasMore || successState.isLoadingMore || successState.isRefreshing) return
        }

        isLoadingVodList = true
        viewModelScope.launch {
            if (isRefresh) {
                currentPage = 1
                _uiState.value = when (previousState) {
                    is HomeUiState.Success -> previousState.copy(
                        isRefreshing = true,
                        isLoadingMore = false
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
                vodRepository.getVodList(baseUrl, pageToLoad, typeId).fold(
                    onSuccess = { response ->
                        val vodList = response.list ?: emptyList()
                        if (vodList.isEmpty()) {
                            _uiState.value = if (pageToLoad == 1) {
                                HomeUiState.Empty
                            } else {
                                (previousState as? HomeUiState.Success)?.copy(
                                    hasMore = false,
                                    isLoadingMore = false,
                                    isRefreshing = false
                                ) ?: HomeUiState.Empty
                            }
                        } else {
                            val existingList = if (pageToLoad == 1) {
                                emptyList()
                            } else {
                                (previousState as? HomeUiState.Success)?.vodList ?: emptyList()
                            }
                            _uiState.value = HomeUiState.Success(
                                vodList = existingList + vodList,
                                hasMore = vodList.size >= 20,
                                isRefreshing = false,
                                isLoadingMore = false
                            )
                            currentPage = pageToLoad + 1
                        }
                    },
                    onFailure = { error ->
                        if (isRefresh) {
                            currentPage = previousPage
                        }
                        _uiState.value = when (previousState) {
                            is HomeUiState.Success -> previousState.copy(
                                hasMore = if (pageToLoad == 1) previousState.hasMore else false,
                                isRefreshing = false,
                                isLoadingMore = false
                            )
                            else -> HomeUiState.Error(error.message ?: "加载失败")
                        }
                    }
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
}
