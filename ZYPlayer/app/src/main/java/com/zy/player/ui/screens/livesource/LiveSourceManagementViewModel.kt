package com.zy.player.ui.screens.livesource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zy.player.data.local.entity.LiveSourceEntity
import com.zy.player.data.repository.LiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveSourceManagementViewModel @Inject constructor(
    private val liveRepository: LiveRepository
) : ViewModel() {

    val sources: StateFlow<List<LiveSourceEntity>> = liveRepository.observeAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSource(name: String, url: String) {
        viewModelScope.launch {
            val maxOrder = sources.value.maxOfOrNull { it.sortOrder } ?: 0
            liveRepository.insertSource(
                LiveSourceEntity(
                    name = name,
                    url = url,
                    enabled = true,
                    sortOrder = maxOrder + 1
                )
            )
        }
    }

    fun updateSource(source: LiveSourceEntity) {
        viewModelScope.launch {
            liveRepository.updateSource(source)
        }
    }

    fun deleteSource(source: LiveSourceEntity) {
        viewModelScope.launch {
            liveRepository.deleteSource(source)
        }
    }

    fun toggleEnabled(source: LiveSourceEntity) {
        viewModelScope.launch {
            liveRepository.updateSource(source.copy(enabled = !source.enabled))
        }
    }

    fun moveSourceUp(source: LiveSourceEntity) {
        viewModelScope.launch {
            liveRepository.moveSourceUp(source, sources.value)
        }
    }

    fun moveSourceDown(source: LiveSourceEntity) {
        viewModelScope.launch {
            liveRepository.moveSourceDown(source, sources.value)
        }
    }
}
