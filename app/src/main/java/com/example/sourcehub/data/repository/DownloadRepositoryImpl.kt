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

/**
 * [DownloadRepository] 的实现，使用 WorkManager + SQLite数据库。
 *
 * ## 架构
 *
 * **持久化**：下载记录通过 [SourcehubDbHelper] 存储在 `downloads` SQLite数据库 表中。
 * 内存中的 [MutableStateFlow] 镜像表数据，以便界面可以响应式地观察变化。
 *
 * **WorkManager**：每个下载由绑定到 [DownloadWorker] 的 [OneTimeWorkRequest] 执行。
 * 工作器通过 [updateProgress] 和 [updateCompleted] 将进度报告回此仓库。
 * 取消/暂停通过取消工作标签（`download_<id>`）实现。
 *
 * **恢复**：当暂停的下载恢复时，使用相同的输入数据将新的 [OneTimeWorkRequest]
 * 入队。[DownloadWorker] 检查本地文件并从上次的字节偏移处恢复。
 *
 * ## 约束
 * - [androidx.work.NetworkType.CONNECTED] -- 下载仅在
 *   设备有网络连接时运行。
 * - 指数退避，从 30 秒开始，用于可重试的失败。
 *
 * ## API 切换
 * [swapApi] 允许在 [MockDownloadApi] 和 [RetrofitDownloadApi] 之间热切换。
 */
class DownloadRepositoryImpl(
    private var downloadApi: DownloadApi,
    private val db: SourcehubDbHelper
) : DownloadRepository {
    /** 在运行时替换底层 API（模拟 <-> 网络层）。 */
    fun swapApi(api: DownloadApi) { downloadApi = api }

    /** `downloads` 表的内存镜像，用于响应式观察。 */
    private val _downloads = MutableStateFlow<List<Download>>(emptyList())

    init {
        // 构造时加载已持久化的下载记录，以便界面立即获得数据。
        kotlinx.coroutines.runBlocking { _downloads.value = db.getDownloads("") }
    }

    /** 从 SQLite数据库 表重新加载内存流。 */
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
            // 步骤 1：从后端获取预签名下载 URL。
            val urlResp = downloadApi.getDownloadUrl(productId)
            if (urlResp.code == 200 && urlResp.data != null) {
                // 步骤 2：创建唯一下载 ID 并持久化记录。
                val downloadId = "dl_${SecurityUtils.generateUuid().take(8)}"
                val d = Download(downloadId, userId, orderId, productId, urlResp.data.fileName, urlResp.data.downloadUrl, "", urlResp.data.fileSize)
                db.insertDownload(d)
                emitFromDb()

                // 步骤 3：构建 WorkManager 约束并将下载工作器入队。
                // NetworkType.CONNECTED 确保下载遵循用户的数据计划。
                val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .setInputData(workDataOf("downloadId" to downloadId, "fileUrl" to urlResp.data.downloadUrl, "fileName" to urlResp.data.fileName))
                    .addTag("download_$downloadId") // 标签使取消/恢复的查找成为可能。
                    .build()
                WorkManager.getInstance(SourcehubApplication.instance).enqueue(workRequest)
                Resource.Success(d)
            } else Resource.Error(urlResp.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "启动失败") }
    }

    override suspend fun getDownloads(userId: String): Flow<List<Download>> =
        _downloads.map { it.filter { d -> d.userId == userId } }

    override suspend fun pauseDownload(downloadId: String) {
        // 通过标签取消 WorkManager 任务 — 这将停止正在进行的传输。
        WorkManager.getInstance(SourcehubApplication.instance).cancelAllWorkByTag("download_$downloadId")
        db.updateDownloadStatus(downloadId, DownloadStatus.PAUSED.name)
        emitFromDb()
    }

    override suspend fun resumeDownload(downloadId: String) {
        val download = _downloads.value.find { it.id == downloadId } ?: return
        // 将状态重置为 PENDING，以便 DownloadWorker 重新拾取。
        db.updateDownloadStatus(downloadId, DownloadStatus.PENDING.name)
        emitFromDb()
        // 使用相同的 fileUrl 和 fileName 重新入队。工作器检查
        // 本地文件并从已有的字节偏移处恢复。
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf("downloadId" to downloadId, "fileUrl" to download.fileUrl, "fileName" to download.fileName))
            .addTag("download_$downloadId")
            .build()
        WorkManager.getInstance(SourcehubApplication.instance).enqueue(workRequest)
    }

    override suspend fun deleteDownload(downloadId: String) {
        // 取消任何正在执行的工作并移除已持久化的记录。
        WorkManager.getInstance(SourcehubApplication.instance).cancelAllWorkByTag("download_$downloadId")
        db.deleteDownload(downloadId)
        emitFromDb()
    }

    override suspend fun updateProgress(downloadId: String, bytes: Long) {
        // 由 DownloadWorker 定期调用，以便界面进度条推进。
        db.updateDownloadProgress(downloadId, bytes, DownloadStatus.DOWNLOADING.name)
        emitFromDb()
    }

    override suspend fun updateCompleted(downloadId: String, localPath: String) {
        // 由 DownloadWorker 在文件传输完成时调用。
        db.markDownloadCompleted(downloadId, localPath)
        emitFromDb()
    }
}
