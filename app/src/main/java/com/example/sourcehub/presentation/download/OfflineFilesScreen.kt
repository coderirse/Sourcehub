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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineFilesScreen(
    onNavigateBack: () -> Unit,
    viewModel: OfflineFilesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                Text("存储空间: ${uiState.storageUsed / 1024 / 1024}MB / ${uiState.storageTotal / 1024 / 1024}MB", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
                LinearProgressIndicator(progress = { uiState.storageUsed.toFloat() / uiState.storageTotal }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                LazyColumn {
                    items(uiState.completedDownloads) { download ->
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (download.fileType.name.contains("PDF")) Icons.Default.PictureAsPdf else Icons.Default.Description, null)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(download.fileName, style = MaterialTheme.typography.bodyMedium)
                                    Text("${download.fileSize / 1024 / 1024}MB", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { viewModel.openFile(context, download.id) }) { Icon(Icons.Default.OpenInNew, "打开") }
                                IconButton(onClick = { viewModel.deleteFile(download.id) }) { Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
}
