package com.example.sourcehub.presentation.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcehub.domain.model.DownloadStatus
import com.example.sourcehub.presentation.common.components.EmptyView

/**
 * 下载管理页面。
 *
 * 展示所有下载任务（包括进行中、暂停、失败、已完成等状态的下载项）。
 * 每个下载卡片显示文件图标、文件名、进度/状态文本，并根据当前状态
 * 提供对应的操作按钮（暂停/继续/打开/删除）。
 *
 * 顶部导航栏右侧提供"离线文件"入口，跳转至 [OfflineFilesScreen]。
 *
 * 页面状态由 [DownloadListViewModel] 驱动，实时通过 [Flow] 订阅下载列表变化。
 *
 * @param onNavigateToOffline 点击"离线文件"按钮时的导航回调。
 * @param viewModel 下载列表的 ViewModel。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    onNavigateToOffline: () -> Unit,
    viewModel: DownloadListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("下载管理") },
                actions = {
                    // 导航到离线文件页面
                    IconButton(onClick = onNavigateToOffline) { Icon(Icons.Default.FolderOpen, "离线文件") }
                }
            )
        }
    ) { padding ->
        if (uiState.downloads.isEmpty()) {
            EmptyView("暂无下载任务", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(uiState.downloads) { download ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            // 文件类型图标：已完成则使用主题色高亮，未完成则使用次要色
                            Icon(
                                Icons.Default.PictureAsPdf, null,
                                tint = if (download.status == DownloadStatus.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(download.fileName, style = MaterialTheme.typography.bodyMedium)
                                // 根据下载状态展示不同的进度/状态 UI
                                when (download.status) {
                                    DownloadStatus.DOWNLOADING -> {
                                        // 正在下载：显示进度条和百分比
                                        LinearProgressIndicator(progress = { download.downloadedBytes.toFloat() / download.fileSize }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                                        Text("${download.downloadedBytes * 100 / download.fileSize}%", style = MaterialTheme.typography.labelSmall)
                                    }
                                    DownloadStatus.COMPLETED -> Text("已完成", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    DownloadStatus.PAUSED -> Text("已暂停", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    DownloadStatus.FAILED -> Text("下载失败", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    DownloadStatus.PENDING -> Text("等待中...", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            // 操作按钮：根据下载状态提供暂停/继续/打开操作
                            when (download.status) {
                                DownloadStatus.DOWNLOADING -> {
                                    IconButton(onClick = { viewModel.pauseDownload(download.id) }) { Icon(Icons.Default.Pause, "暂停") }
                                }
                                DownloadStatus.PAUSED, DownloadStatus.FAILED -> {
                                    // 暂停和失败状态均可恢复下载
                                    IconButton(onClick = { viewModel.resumeDownload(download.id) }) { Icon(Icons.Default.PlayArrow, "继续") }
                                }
                                DownloadStatus.COMPLETED -> {
                                    IconButton(onClick = { viewModel.openFile(download.id) }) { Icon(Icons.Default.OpenInNew, "打开") }
                                }
                                else -> {} // PENDING 状态不显示操作按钮
                            }
                            // 删除按钮：所有状态均可删除
                            IconButton(onClick = { viewModel.deleteDownload(download.id) }) {
                                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
