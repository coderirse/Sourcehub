package com.example.sourcehub.domain.repository

import com.example.sourcehub.domain.model.Download
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    suspend fun getDownloadUrl(productId: String): Resource<String>
    suspend fun startDownload(userId: String, orderId: String, productId: String): Resource<Download>
    suspend fun getDownloads(userId: String): Flow<List<Download>>
    suspend fun pauseDownload(downloadId: String)
    suspend fun resumeDownload(downloadId: String)
    suspend fun deleteDownload(downloadId: String)
}
