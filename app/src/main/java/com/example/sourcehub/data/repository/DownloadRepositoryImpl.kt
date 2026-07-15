package com.example.sourcehub.data.repository

import androidx.work.*
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.data.local.db.SourcehubDbHelper
import com.example.sourcehub.data.remote.api.DownloadApi
import com.example.sourcehub.domain.model.Download
import com.example.sourcehub.domain.model.DownloadStatus
import com.example.sourcehub.domain.repository.DownloadRepository
import com.example.sourcehub.presentation.common.state.Resource
import com.example.sourcehub.security.SecurityUtils
import com.example.sourcehub.worker.DownloadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class DownloadRepositoryImpl(
    private var downloadApi: DownloadApi,
    private val db: SourcehubDbHelper
) : DownloadRepository {
    fun swapApi(api: DownloadApi) { downloadApi = api }
    private val _downloads = MutableStateFlow<List<Download>>(emptyList())

    init {
        kotlinx.coroutines.runBlocking { _downloads.value = db.getDownloads("") }
    }

    private fun emitFromDb() {
        kotlinx.coroutines.runBlocking { _downloads.value = db.getDownloads("") }
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
                val downloadId = "dl_${SecurityUtils.generateUuid().take(8)}"
                val d = Download(downloadId, userId, orderId, productId, urlResp.data.fileName, urlResp.data.downloadUrl, "", urlResp.data.fileSize)
                db.insertDownload(d)
                emitFromDb()

                val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .setInputData(workDataOf("downloadId" to downloadId, "fileUrl" to urlResp.data.downloadUrl, "fileName" to urlResp.data.fileName))
                    .addTag("download_$downloadId")
                    .build()
                WorkManager.getInstance(SourcehubApplication.instance).enqueue(workRequest)
                Resource.Success(d)
            } else Resource.Error(urlResp.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "启动失败") }
    }

    override suspend fun getDownloads(userId: String): Flow<List<Download>> = _downloads.map { it.filter { d -> d.userId == userId } }

    override suspend fun pauseDownload(downloadId: String) {
        WorkManager.getInstance(SourcehubApplication.instance).cancelAllWorkByTag("download_$downloadId")
        db.updateDownloadStatus(downloadId, DownloadStatus.PAUSED.name)
        emitFromDb()
    }

    override suspend fun resumeDownload(downloadId: String) {
        val download = _downloads.value.find { it.id == downloadId } ?: return
        db.updateDownloadStatus(downloadId, DownloadStatus.PENDING.name)
        emitFromDb()
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf("downloadId" to downloadId, "fileUrl" to download.fileUrl, "fileName" to download.fileName))
            .addTag("download_$downloadId")
            .build()
        WorkManager.getInstance(SourcehubApplication.instance).enqueue(workRequest)
    }

    override suspend fun deleteDownload(downloadId: String) {
        WorkManager.getInstance(SourcehubApplication.instance).cancelAllWorkByTag("download_$downloadId")
        db.deleteDownload(downloadId)
        emitFromDb()
    }

    override suspend fun updateProgress(downloadId: String, bytes: Long) {
        db.updateDownloadProgress(downloadId, bytes, DownloadStatus.DOWNLOADING.name)
        emitFromDb()
    }

    override suspend fun updateCompleted(downloadId: String, localPath: String) {
        db.markDownloadCompleted(downloadId, localPath)
        emitFromDb()
    }
}
