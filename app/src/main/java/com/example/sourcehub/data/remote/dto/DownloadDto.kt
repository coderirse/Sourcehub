package com.example.sourcehub.data.remote.dto

data class DownloadUrlResponse(
    val productId: String,
    val downloadUrl: String,
    val fileName: String,
    val fileSize: Long,
    val expiresAt: Long
)

data class DownloadProgressRequest(
    val downloadId: String,
    val progress: Float,
    val downloadedBytes: Long
)
