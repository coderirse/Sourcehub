package com.example.sourcehub.presentation.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.Download
import com.example.sourcehub.domain.model.DownloadStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OfflineFilesUiState(
    val completedDownloads: List<Download> = emptyList(),
    val storageUsed: Long = 0L,
    val storageTotal: Long = 1024L * 1024 * 1024 // 1GB placeholder
)

class OfflineFilesViewModel : ViewModel() {
    private val downloadRepository = SourcehubApplication.instance.appContainer.downloadRepository
    private val userId = SourcehubApplication.instance.appContainer.authRepository.getUserId()
    private val _uiState = MutableStateFlow(OfflineFilesUiState())
    val uiState: StateFlow<OfflineFilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadRepository.getDownloads(userId).collect { downloads ->
                val completed = downloads.filter { it.status == DownloadStatus.COMPLETED }
                _uiState.update {
                    it.copy(
                        completedDownloads = completed,
                        storageUsed = completed.sumOf { d -> d.fileSize }
                    )
                }
            }
        }
    }

    fun openFile(downloadId: String) { /* Decrypt and open */ }
    fun deleteFile(downloadId: String) {
        viewModelScope.launch { downloadRepository.deleteDownload(downloadId) }
    }
}
