package com.zy.player.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zy.player.data.repository.LiveRepository
import com.zy.player.domain.model.LiveChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LiveUiState {
    object Loading : LiveUiState()
    data class Success(val channels: List<LiveChannel>) : LiveUiState()
    data class Error(val message: String) : LiveUiState()
    object Empty : LiveUiState()
}

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val liveRepository: LiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LiveUiState>(LiveUiState.Loading)
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private val _currentSourceId = MutableStateFlow<Long?>(null)
    val currentSourceId: StateFlow<Long?> = _currentSourceId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()

    private var allChannels = emptyList<LiveChannel>()

    val sources = liveRepository.observeAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            sources.collectLatest { sourceList ->
                val enabledSources = sourceList.filter { it.enabled }
                if (enabledSources.isNotEmpty() && _currentSourceId.value == null) {
                    selectSource(enabledSources.first().id)
                }
            }
        }
    }

    fun selectSource(sourceId: Long) {
        viewModelScope.launch {
            _currentSourceId.value = sourceId
            val source = sources.value.find { it.id == sourceId }
            if (source != null) {
                loadChannels(source.url)
            }
        }
    }

    private fun loadChannels(url: String) {
        viewModelScope.launch {
            _uiState.value = LiveUiState.Loading
            liveRepository.fetchAndParseChannels(url).fold(
                onSuccess = { channels ->
                    allChannels = channels
                    applyFilters()
                },
                onFailure = { error ->
                    _uiState.value = LiveUiState.Error(error.message ?: "加载失败")
                }
            )
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun selectGroup(group: String?) {
        _selectedGroup.value = group
        applyFilters()
    }

    private fun applyFilters() {
        val query = _searchQuery.value
        val group = _selectedGroup.value

        val filtered = allChannels.filter { channel ->
            val matchesQuery = query.isBlank() || channel.name.contains(query, ignoreCase = true)
            val matchesGroup = group == null || channel.group == group
            matchesQuery && matchesGroup
        }

        _uiState.value = if (filtered.isEmpty()) {
            LiveUiState.Empty
        } else {
            LiveUiState.Success(filtered)
        }
    }

    fun getGroups(): List<String> {
        return allChannels.map { it.group }.distinct().sorted()
    }
}
