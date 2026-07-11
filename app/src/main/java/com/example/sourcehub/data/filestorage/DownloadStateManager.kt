package com.example.sourcehub.data.filestorage

import com.example.sourcehub.domain.model.DownloadState
import com.example.sourcehub.domain.model.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DownloadStateManager {
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    fun updateProgress(downloadId: String, progress: Float, downloadedBytes: Long, totalBytes: Long, speed: Long) {
        _downloadStates.value = _downloadStates.value + (downloadId to DownloadState(
            downloadId = downloadId,
            progress = progress,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            speed = speed,
            status = DownloadStatus.DOWNLOADING
        ))
    }

    fun updateStatus(downloadId: String, status: DownloadStatus) {
        val current = _downloadStates.value[downloadId] ?: return
        _downloadStates.value = _downloadStates.value + (downloadId to current.copy(status = status))
    }

    fun remove(downloadId: String) {
        _downloadStates.value = _downloadStates.value - downloadId
    }
}
