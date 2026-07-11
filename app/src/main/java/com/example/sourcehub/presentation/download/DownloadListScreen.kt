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
                            Icon(
                                Icons.Default.PictureAsPdf, null,
                                tint = if (download.status == DownloadStatus.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(download.fileName, style = MaterialTheme.typography.bodyMedium)
                                when (download.status) {
                                    DownloadStatus.DOWNLOADING -> {
                                        LinearProgressIndicator(progress = { download.downloadedBytes.toFloat() / download.fileSize }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                                        Text("${download.downloadedBytes * 100 / download.fileSize}%", style = MaterialTheme.typography.labelSmall)
                                    }
                                    DownloadStatus.COMPLETED -> Text("已完成", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    DownloadStatus.PAUSED -> Text("已暂停", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    DownloadStatus.FAILED -> Text("下载失败", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    DownloadStatus.PENDING -> Text("等待中...", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            when (download.status) {
                                DownloadStatus.DOWNLOADING -> {
                                    IconButton(onClick = { viewModel.pauseDownload(download.id) }) { Icon(Icons.Default.Pause, "暂停") }
                                }
                                DownloadStatus.PAUSED, DownloadStatus.FAILED -> {
                                    IconButton(onClick = { viewModel.resumeDownload(download.id) }) { Icon(Icons.Default.PlayArrow, "继续") }
                                }
                                DownloadStatus.COMPLETED -> {
                                    IconButton(onClick = { viewModel.openFile(download.id) }) { Icon(Icons.Default.OpenInNew, "打开") }
                                }
                                else -> {}
                            }
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
