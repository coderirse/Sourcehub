package com.example.sourcehub.presentation.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcehub.domain.model.DownloadStatus
import com.example.sourcehub.presentation.common.components.EmptyView

/**
 * 离线文件页面。
 *
 * 展示所有已完成下载、可供离线访问的文件列表。
 * 顶部显示当前存储空间使用情况（已用 / 总量 + 进度条），
 * 下方列表展示每个离线文件的文件名、大小以及打开/删除操作按钮。
 *
 * 打开文件时，[OfflineFilesViewModel.openFile] 会解密文件并使用系统对应应用打开。
 *
 * 页面状态由 [OfflineFilesViewModel] 驱动，通过 [OfflineFilesUiState] 暴露
 * 已完成下载列表和存储空间统计数据。
 *
 * @param onNavigateBack 返回上一页的回调。
 * @param viewModel 离线文件的 ViewModel。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineFilesScreen(
    onNavigateBack: () -> Unit,
    viewModel: OfflineFilesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 获取当前 Android Context，供打开文件（Intent）使用
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("离线文件") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        if (uiState.completedDownloads.isEmpty()) {
            EmptyView("暂无离线文件", modifier = Modifier.padding(padding))
        } else {
            Column(Modifier.padding(padding)) {
                // 存储空间信息：已用MB / 总量MB，附带进度条
                Text("存储空间: ${uiState.storageUsed / 1024 / 1024}MB / ${uiState.storageTotal / 1024 / 1024}MB", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
                LinearProgressIndicator(progress = { uiState.storageUsed.toFloat() / uiState.storageTotal }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                LazyColumn {
                    items(uiState.completedDownloads) { download ->
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                // 根据文件类型显示不同图标：PDF 文件使用 PDF 图标，其他使用通用文件图标
                                Icon(if (download.fileType.name.contains("PDF")) Icons.Default.PictureAsPdf else Icons.Default.Description, null)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(download.fileName, style = MaterialTheme.typography.bodyMedium)
                                    // 文件大小以 MB 显示
                                    Text("${download.fileSize / 1024 / 1024}MB", style = MaterialTheme.typography.labelSmall)
                                }
                                // 打开文件：解密并通过系统 Intent 打开
                                IconButton(onClick = { viewModel.openFile(context, download.id) }) { Icon(Icons.Default.OpenInNew, "打开") }
                                // 删除离线文件及其下载记录
                                IconButton(onClick = { viewModel.deleteFile(download.id) }) { Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
}
