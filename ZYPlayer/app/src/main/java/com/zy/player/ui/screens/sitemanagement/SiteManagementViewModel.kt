package com.zy.player.ui.screens.sitemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zy.player.data.local.entity.VideoSiteEntity
import com.zy.player.data.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SiteManagementViewModel @Inject constructor(
    private val siteRepository: SiteRepository
) : ViewModel() {

    val sites: StateFlow<List<VideoSiteEntity>> = siteRepository.observeAllSites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSite(name: String, url: String) {
        viewModelScope.launch {
            val maxOrder = sites.value.maxOfOrNull { it.sortOrder } ?: 0
            siteRepository.insertSite(
                VideoSiteEntity(
                    name = name,
                    apiUrl = url,
                    enabled = true,
                    sortOrder = maxOrder + 1
                )
            )
        }
    }

    fun updateSite(site: VideoSiteEntity) {
        viewModelScope.launch {
            siteRepository.updateSite(site)
        }
    }

    fun deleteSite(site: VideoSiteEntity) {
        viewModelScope.launch {
            siteRepository.deleteSite(site)
        }
    }

    fun toggleEnabled(site: VideoSiteEntity) {
        viewModelScope.launch {
            siteRepository.updateSite(site.copy(enabled = !site.enabled))
        }
    }

    fun moveSiteUp(site: VideoSiteEntity) {
        viewModelScope.launch {
            siteRepository.moveSiteUp(site, sites.value)
        }
    }

    fun moveSiteDown(site: VideoSiteEntity) {
        viewModelScope.launch {
            siteRepository.moveSiteDown(site, sites.value)
        }
    }

    fun clearAllSites() {
        viewModelScope.launch {
            siteRepository.clearAllSites()
        }
    }
}
