package com.zy.player.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zy.player.data.repository.HistoryRepository
import com.zy.player.data.repository.LiveRepository
import com.zy.player.data.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val siteRepository: SiteRepository,
    private val liveRepository: LiveRepository
) : ViewModel() {
    private val _maintenanceMessage = MutableStateFlow<String?>(null)
    val maintenanceMessage: StateFlow<String?> = _maintenanceMessage.asStateFlow()

    fun resetApp() {
        viewModelScope.launch {
            runCatching {
                historyRepository.clearAllHistory()
                siteRepository.resetToDefaults()
                liveRepository.resetToDefaults()
            }.onSuccess {
                _maintenanceMessage.value = "已清空播放历史，并恢复默认视频源与直播源。"
            }.onFailure { error ->
                _maintenanceMessage.value = "重置失败：${error.message ?: "未知错误"}"
            }
        }
    }

    fun consumeMaintenanceMessage() {
        _maintenanceMessage.value = null
    }
}
