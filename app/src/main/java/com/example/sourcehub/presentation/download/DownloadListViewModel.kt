package com.example.sourcehub.presentation.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.Download
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DownloadListUiState(val downloads: List<Download> = emptyList())

class DownloadListViewModel : ViewModel() {
    private val appContainer = SourcehubApplication.instance.appContainer
    private val downloadRepository = appContainer.downloadRepository
    private val userId = appContainer.authRepository.getUserId()
    private val _uiState = MutableStateFlow(DownloadListUiState())
    val uiState: StateFlow<DownloadListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadRepository.getDownloads(userId).collect { downloads ->
                _uiState.update { it.copy(downloads = downloads) }
            }
        }
    }

    fun pauseDownload(downloadId: String) {
        viewModelScope.launch { downloadRepository.pauseDownload(downloadId) }
    }

    fun resumeDownload(downloadId: String) {
        viewModelScope.launch { downloadRepository.resumeDownload(downloadId) }
    }

    fun deleteDownload(downloadId: String) {
        viewModelScope.launch { downloadRepository.deleteDownload(downloadId) }
    }

    fun openFile(downloadId: String) {
        // In production: decrypt file and open with appropriate app
    }
}
