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
        val hasMore: Boolean
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
        if (isRefresh) {
            currentPage = 1
        }

        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = HomeUiState.Loading
            }

            val baseUrl = currentBaseUrl ?: return@launch
            val typeId = _selectedCategoryId.value?.takeIf { it != 0 }

            vodRepository.getVodList(baseUrl, currentPage, typeId).fold(
                onSuccess = { response ->
                    val vodList = response.list ?: emptyList()
                    if (vodList.isEmpty()) {
                        _uiState.value = if (currentPage == 1) HomeUiState.Empty else {
                            (_uiState.value as? HomeUiState.Success)?.copy(hasMore = false) ?: HomeUiState.Empty
                        }
                    } else {
                        val existingList = if (isRefresh) emptyList() else {
                            (_uiState.value as? HomeUiState.Success)?.vodList ?: emptyList()
                        }
                        _uiState.value = HomeUiState.Success(
                            vodList = existingList + vodList,
                            hasMore = vodList.size >= 20
                        )
                        currentPage++
                    }
                },
                onFailure = { error ->
                    _uiState.value = HomeUiState.Error(error.message ?: "加载失败")
                }
            )
        }
    }

    fun refresh() {
        loadVodList(isRefresh = true)
    }
}
