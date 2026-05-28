package com.zy.player.ui.screens.sitemanagement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zy.player.data.local.entity.VideoSiteEntity
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.components.CinemaMessage
import com.zy.player.ui.components.PageHeader
import com.zy.player.ui.components.SourceEditorDialog
import com.zy.player.ui.components.SourceCheckResultDialog
import com.zy.player.ui.theme.AppColors
import com.zy.player.ui.theme.Dimens

@Composable
fun SiteManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: SiteManagementViewModel = hiltViewModel()
) {
    val sites by viewModel.sites.collectAsState()
    val checkingSiteId by viewModel.checkingSiteId.collectAsState()
    val checkResultDialog by viewModel.checkResultDialog.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSite by remember { mutableStateOf<VideoSiteEntity?>(null) }
    var deletingSite by remember { mutableStateOf<VideoSiteEntity?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = "视频源管理",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加",
                            tint = AppColors.TextPrimary
                        )
                    }
                    IconButton(
                        onClick = { showClearDialog = true },
                        enabled = sites.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "清空",
                            tint = if (sites.isNotEmpty()) AppColors.TextPrimary else AppColors.TextTertiary
                        )
                    }
                }
            )

            if (sites.isEmpty()) {
                CinemaMessage(
                    modifier = Modifier.fillMaxSize(),
                    title = "暂无视频源",
                    message = "添加资源站接口后，首页和搜索会自动使用启用的源。",
                    actionText = "添加视频源",
                    onAction = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(Dimens.paddingMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.paddingSmall)
                ) {
                    itemsIndexed(sites) { index, site ->
                        SiteItem(
                            site = site,
                            canMoveUp = index > 0,
                            canMoveDown = index < sites.lastIndex,
                            onToggleEnabled = { viewModel.toggleEnabled(site) },
                            onEdit = { editingSite = site },
                            onDelete = { deletingSite = site },
                            onMoveUp = { viewModel.moveSiteUp(site) },
                            onMoveDown = { viewModel.moveSiteDown(site) },
                            onCheck = { viewModel.checkSite(site) },
                            isChecking = checkingSiteId == site.id,
                            isCheckEnabled = checkingSiteId == null
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        SourceEditorDialog(
            title = "添加视频源",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url ->
                viewModel.addSite(name, url)
                showAddDialog = false
            }
        )
    }

    editingSite?.let { site ->
        SourceEditorDialog(
            title = "编辑视频源",
            initialName = site.name,
            initialUrl = site.apiUrl,
            onDismiss = { editingSite = null },
            onConfirm = { name, url ->
                viewModel.updateSite(site.copy(name = name, apiUrl = url))
                editingSite = null
            }
        )
    }

    deletingSite?.let { site ->
        ConfirmDialog(
            title = "删除视频源",
            message = "确定要删除 ${site.name} 吗？",
            onDismiss = { deletingSite = null },
            onConfirm = {
                viewModel.deleteSite(site)
                deletingSite = null
            }
        )
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = "清空视频源",
            message = "确定要清空所有视频源吗？可在设置页重置应用恢复默认源。",
            onDismiss = { showClearDialog = false },
            onConfirm = {
                viewModel.clearAllSites()
                showClearDialog = false
            }
        )
    }

    checkResultDialog?.let { state ->
        SourceCheckResultDialog(
            state = state,
            onDismiss = viewModel::dismissCheckResultDialog
        )
    }
}

@Composable
private fun SiteItem(
    site: VideoSiteEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onCheck: () -> Unit,
    isChecking: Boolean,
    isCheckEnabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.045f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, AppColors.Divider)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = site.name,
                        color = AppColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = site.apiUrl,
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "状态 ${site.lastCheckStatus}",
                        color = AppColors.TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = site.enabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ManagementIconButton(
                    icon = Icons.Default.CheckCircle,
                    contentDescription = "检测",
                    enabled = isCheckEnabled || isChecking,
                    isLoading = isChecking,
                    onClick = onCheck
                )
                ManagementIconButton(
                    icon = Icons.Default.ArrowUpward,
                    contentDescription = "上移",
                    enabled = canMoveUp,
                    onClick = onMoveUp
                )
                ManagementIconButton(
                    icon = Icons.Default.ArrowDownward,
                    contentDescription = "下移",
                    enabled = canMoveDown,
                    onClick = onMoveDown
                )
                ManagementIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "编辑",
                    onClick = onEdit
                )
                ManagementIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "删除",
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun ManagementIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier.size(40.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AppColors.Primary,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) AppColors.TextPrimary else AppColors.TextTertiary
            )
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        titleContentColor = AppColors.TextPrimary,
        textContentColor = AppColors.TextSecondary,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
