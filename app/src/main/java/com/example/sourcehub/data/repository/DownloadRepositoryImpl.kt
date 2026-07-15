package com.example.sourcehub.data.repository

import com.example.sourcehub.data.local.persistence.JsonPersistenceManager
import com.example.sourcehub.data.local.persistence.toDownload
import com.example.sourcehub.data.local.persistence.toJson
import com.example.sourcehub.data.remote.api.DownloadApi
import com.example.sourcehub.domain.model.Download
import com.example.sourcehub.domain.model.DownloadStatus
import com.example.sourcehub.domain.repository.DownloadRepository
import com.example.sourcehub.presentation.common.state.Resource
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class DownloadRepositoryImpl(
    private val downloadApi: DownloadApi,
    private val persistence: JsonPersistenceManager
) : DownloadRepository {
    private val _downloads = MutableStateFlow<List<Download>>(emptyList())

    init {
        kotlinx.coroutines.runBlocking {
            val arr = persistence.loadArray("downloads")
            if (arr != null) {
                val loaded = mutableListOf<Download>()
                for (i in 0 until arr.length()) loaded.add(arr.getJSONObject(i).toDownload())
                _downloads.value = loaded
            }
        }
    }

    private suspend fun persist() {
        val arr = JSONArray()
        _downloads.value.forEach { arr.put(it.toJson()) }
        persistence.saveArray("downloads", arr)
    }

    override suspend fun getDownloadUrl(productId: String): Resource<String> {
        return try {
            val resp = downloadApi.getDownloadUrl(productId)
            if (resp.code == 200 && resp.data != null) Resource.Success(resp.data.downloadUrl)
            else Resource.Error(resp.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "获取失败") }
    }

    override suspend fun startDownload(userId: String, orderId: String, productId: String): Resource<Download> {
        return try {
            val urlResp = downloadApi.getDownloadUrl(productId)
            if (urlResp.code == 200 && urlResp.data != null) {
                val d = Download("dl_${SecurityUtils.generateUuid().take(8)}", userId, orderId, productId, urlResp.data.fileName, urlResp.data.downloadUrl, "", urlResp.data.fileSize)
                _downloads.value = _downloads.value + d
                persist()
                Resource.Success(d)
            } else Resource.Error(urlResp.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "启动失败") }
    }

    override suspend fun getDownloads(userId: String): Flow<List<Download>> = _downloads.map { it.filter { d -> d.userId == userId } }
    override suspend fun pauseDownload(downloadId: String) { _downloads.value = _downloads.value.map { if (it.id == downloadId) it.copy(status = DownloadStatus.PAUSED) else it }; persist() }
    override suspend fun resumeDownload(downloadId: String) { _downloads.value = _downloads.value.map { if (it.id == downloadId) it.copy(status = DownloadStatus.PENDING) else it }; persist() }
    override suspend fun deleteDownload(downloadId: String) { _downloads.value = _downloads.value.filter { it.id != downloadId }; persist() }
}
