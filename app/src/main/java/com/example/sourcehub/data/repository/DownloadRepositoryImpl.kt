package com.example.sourcehub.data.repository

import com.example.sourcehub.data.remote.api.DownloadApi
import com.example.sourcehub.domain.model.Download
import com.example.sourcehub.domain.model.DownloadStatus
import com.example.sourcehub.domain.model.FileType
import com.example.sourcehub.domain.repository.DownloadRepository
import com.example.sourcehub.presentation.common.state.Resource
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class DownloadRepositoryImpl(
    private val downloadApi: DownloadApi
) : DownloadRepository {

    private val _downloads = MutableStateFlow<List<Download>>(emptyList())

    override suspend fun getDownloadUrl(productId: String): Resource<String> {
        return try {
            val resp = downloadApi.getDownloadUrl(productId)
            if (resp.code == 200 && resp.data != null) Resource.Success(resp.data.downloadUrl)
            else Resource.Error(resp.message)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "获取下载链接失败")
        }
    }

    override suspend fun startDownload(userId: String, orderId: String, productId: String): Resource<Download> {
        return try {
            val urlResp = downloadApi.getDownloadUrl(productId)
            if (urlResp.code == 200 && urlResp.data != null) {
                val download = Download(
                    id = "dl_${SecurityUtils.generateUuid().take(8)}",
                    userId = userId, orderId = orderId, productId = productId,
                    fileName = urlResp.data.fileName, fileUrl = urlResp.data.downloadUrl,
                    fileSize = urlResp.data.fileSize
                )
                _downloads.value = _downloads.value + download
                Resource.Success(download)
            } else Resource.Error(urlResp.message)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "启动下载失败")
        }
    }

    override suspend fun getDownloads(userId: String): Flow<List<Download>> {
        return _downloads.map { it.filter { d -> d.userId == userId } }
    }

    override suspend fun pauseDownload(downloadId: String) {
        _downloads.value = _downloads.value.map {
            if (it.id == downloadId) it.copy(status = DownloadStatus.PAUSED) else it
        }
    }

    override suspend fun resumeDownload(downloadId: String) {
        _downloads.value = _downloads.value.map {
            if (it.id == downloadId) it.copy(status = DownloadStatus.PENDING) else it
        }
    }

    override suspend fun deleteDownload(downloadId: String) {
        _downloads.value = _downloads.value.filter { it.id != downloadId }
    }
}
