package com.zy.player.ui.screens.sitemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zy.player.data.local.entity.VideoSiteEntity
import com.zy.player.data.repository.SiteRepository
import com.zy.player.data.repository.VideoSiteCheckResponse
import com.zy.player.data.repository.VodRepository
import com.zy.player.data.repository.classifySourceCheckFailure
import com.zy.player.data.repository.sourceCheckFailureMessage
import com.zy.player.data.repository.sourceCheckReturnedContent
import com.zy.player.ui.components.SourceCheckResultDialogState
import com.zy.player.ui.components.SourceCheckSummaryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SiteManagementViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val vodRepository: VodRepository
) : ViewModel() {

    val sites: StateFlow<List<VideoSiteEntity>> = siteRepository.observeAllSites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _checkingSiteId = MutableStateFlow<Long?>(null)
    val checkingSiteId: StateFlow<Long?> = _checkingSiteId.asStateFlow()

    private val _checkResultDialog = MutableStateFlow<SourceCheckResultDialogState?>(null)
    val checkResultDialog: StateFlow<SourceCheckResultDialogState?> = _checkResultDialog.asStateFlow()

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

    fun checkSite(site: VideoSiteEntity) {
        if (_checkingSiteId.value != null) return

        viewModelScope.launch {
            _checkingSiteId.value = site.id
            try {
                val result = vodRepository.checkVideoSite(site.apiUrl)
                result.fold(
                    onSuccess = { response ->
                        siteRepository.updateSite(
                            site.copy(
                                lastCheckStatus = "可用",
                                lastCheckTime = System.currentTimeMillis()
                            )
                        )
                        _checkResultDialog.value = buildSuccessDialog(site, response)
                    },
                    onFailure = { error ->
                        val reason = classifySourceCheckFailure(error)
                        siteRepository.updateSite(
                            site.copy(
                                lastCheckStatus = reason.statusText,
                                lastCheckTime = System.currentTimeMillis()
                            )
                        )
                        _checkResultDialog.value = SourceCheckResultDialogState(
                            title = "视频源检测失败",
                            sourceName = site.name,
                            success = false,
                            message = "${reason.label}：${sourceCheckFailureMessage(reason, error)}",
                            summary = listOf(
                                SourceCheckSummaryItem("检测地址", site.apiUrl),
                                SourceCheckSummaryItem("失败分类", reason.label)
                            ),
                            returnedContent = sourceCheckReturnedContent(error)?.toDialogContent()
                        )
                    }
                )
            } finally {
                _checkingSiteId.value = null
            }
        }
    }

    fun dismissCheckResultDialog() {
        _checkResultDialog.value = null
    }

    private fun buildSuccessDialog(
        site: VideoSiteEntity,
        response: VideoSiteCheckResponse
    ): SourceCheckResultDialogState {
        val apiResponse = response.response
        val sampleVideos = apiResponse.list.orEmpty()
            .take(5)
            .joinToString("、") { it.vod_name }
            .ifBlank { "无" }
        val sampleClasses = apiResponse.`class`.orEmpty()
            .take(5)
            .joinToString("、") { it.type_name }
            .ifBlank { "无" }

        return SourceCheckResultDialogState(
            title = "视频源检测成功",
            sourceName = site.name,
            success = true,
            message = "接口可访问，返回数据已成功解析。",
            summary = listOf(
                SourceCheckSummaryItem("检测地址", site.apiUrl),
                SourceCheckSummaryItem("HTTP 状态", response.httpCode.toString()),
                SourceCheckSummaryItem("内容类型", response.contentType ?: "未知"),
                SourceCheckSummaryItem("接口状态", listOfNotNull(apiResponse.code?.toString(), apiResponse.msg).joinToString(" / ").ifBlank { "无" }),
                SourceCheckSummaryItem("分页信息", "page=${apiResponse.page ?: "-"} / pagecount=${apiResponse.pagecount ?: "-"} / total=${apiResponse.total ?: "-"}"),
                SourceCheckSummaryItem("列表数量", apiResponse.list.orEmpty().size.toString()),
                SourceCheckSummaryItem("分类数量", apiResponse.`class`.orEmpty().size.toString()),
                SourceCheckSummaryItem("样例影片", sampleVideos),
                SourceCheckSummaryItem("样例分类", sampleClasses)
            ),
            returnedContent = response.rawContent.toDialogContent()
        )
    }

    private fun String.toDialogContent(maxLength: Int = 4000): String {
        if (length <= maxLength) return this
        return take(maxLength) + "\n\n...返回内容较长，仅展示前 ${maxLength} 字。"
    }
}
