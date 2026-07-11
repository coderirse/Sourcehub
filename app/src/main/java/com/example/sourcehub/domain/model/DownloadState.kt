package com.example.sourcehub.domain.model

data class DownloadState(
    val downloadId: String = "",
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speed: Long = 0L, // bytes per second
    val status: DownloadStatus = DownloadStatus.PENDING
)
