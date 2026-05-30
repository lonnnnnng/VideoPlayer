package com.zy.player.ui.screens.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zy.player.data.local.entity.PodcastSubscriptionEntity
import com.zy.player.data.repository.PodcastRepository
import com.zy.player.domain.model.PodcastFeed
import com.zy.player.domain.model.PodcastLibraryEpisode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PodcastUiState(
    val isAdding: Boolean = false,
    val isLoadingFeed: Boolean = false,
    val isRefreshingLibrary: Boolean = false,
    val selectedSubscriptionId: Long? = null,
    val selectedFeed: PodcastFeed? = null,
    val libraryEpisodes: List<PodcastLibraryEpisode> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PodcastUiState())
    val uiState: StateFlow<PodcastUiState> = _uiState.asStateFlow()

    val subscriptions = podcastRepository.observeSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshLibrary()
    }

    fun addSubscription(url: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAdding = true, message = null)
            podcastRepository.addSubscription(trimmedUrl).fold(
                onSuccess = { subscription ->
                    _uiState.value = _uiState.value.copy(
                        isAdding = false,
                        message = "已订阅 ${subscription.title}"
                    )
                    refreshLibrary()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isAdding = false,
                        message = error.message ?: "订阅添加失败"
                    )
                }
            )
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshingLibrary = true, message = null)
            podcastRepository.fetchLibraryEpisodes().fold(
                onSuccess = { episodes ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshingLibrary = false,
                        libraryEpisodes = episodes
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshingLibrary = false,
                        message = error.message ?: "聚合节目刷新失败"
                    )
                }
            )
        }
    }

    fun openSubscription(subscription: PodcastSubscriptionEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingFeed = true,
                selectedSubscriptionId = subscription.id,
                selectedFeed = null,
                message = null
            )
            podcastRepository.fetchPodcastFeed(subscription.url).fold(
                onSuccess = { feed ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingFeed = false,
                        selectedFeed = feed
                    )
                    podcastRepository.refreshSubscription(subscription)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingFeed = false,
                        message = error.message ?: "订阅刷新失败"
                    )
                }
            )
        }
    }

    fun closeSubscription() {
        _uiState.value = _uiState.value.copy(
            selectedSubscriptionId = null,
            selectedFeed = null,
            isLoadingFeed = false,
            message = null
        )
    }

    fun deleteSubscription(subscription: PodcastSubscriptionEntity) {
        viewModelScope.launch {
            podcastRepository.deleteSubscription(subscription)
            val current = _uiState.value
            _uiState.value = current.copy(
                selectedSubscriptionId = if (current.selectedSubscriptionId == subscription.id) null else current.selectedSubscriptionId,
                selectedFeed = if (current.selectedSubscriptionId == subscription.id) null else current.selectedFeed,
                message = "已删除 ${subscription.title}"
            )
            refreshLibrary()
        }
    }

    fun clearMessage() {
        if (_uiState.value.message != null) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }
}
