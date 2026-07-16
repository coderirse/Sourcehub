package com.example.sourcehub.domain.repository

import com.example.sourcehub.domain.model.Download
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.Flow

/**
 * 文件下载操作的契约接口。
 *
 * 管理完整的下载生命周期：从后端获取下载 URL、
 * 通过 WorkManager 将 [DownloadWorker] 任务入队，以及
 * 将进度/状态更新持久化到 SQLite数据库。下载列表通过
 * 响应式 [Flow] 暴露，使界面能够实时更新。
 */
interface DownloadRepository {

    /**
     * 获取已购商品的预签名下载 URL。
     * @param productId 要下载的商品。
     */
    suspend fun getDownloadUrl(productId: String): Resource<String>

    /**
     * 启动新的下载任务。
     *
     * 在 SQLite数据库 中创建一条 [Download] 记录，获取下载 URL，
     * 并通过 WorkManager 将一个带网络连接约束和指数退避策略的
     * [OneTimeWorkRequest] 入队。
     *
     * @param userId 启动下载的用户。
     * @param orderId 授权该下载的已完成订单。
     * @param productId 要下载的商品。
     * @return 包含已创建[Download]任务的 [Resource.Success]，或 [Resource.Error]。
     */
    suspend fun startDownload(userId: String, orderId: String, productId: String): Resource<Download>

    /**
     * 以响应式 [Flow] 方式观察指定用户的所有下载。
     * 当任何下载的状态发生变化时，发出完整列表。
     */
    suspend fun getDownloads(userId: String): Flow<List<Download>>

    /**
     * 通过取消其 WorkManager 任务来暂停正在进行的下载。
     * 下载状态在 SQLite数据库 中更新为 PAUSED。
     */
    suspend fun pauseDownload(downloadId: String)

    /**
     * 通过重新入队一个新的 WorkManager 任务来恢复已暂停的下载。
     * 下载状态重置为 PENDING，以便 [DownloadWorker] 接手处理。
     */
    suspend fun resumeDownload(downloadId: String)

    /**
     * 删除下载：取消其 WorkManager 任务并移除 SQLite数据库 记录。
     */
    suspend fun deleteDownload(downloadId: String)

    /**
     * 更新已下载的字节数，并将状态设置为 DOWNLOADING。
     * 由 [DownloadWorker] 在传输过程中定期调用。
     */
    suspend fun updateProgress(downloadId: String, bytes: Long)

    /**
     * 将下载标记为完成，并记录本地文件路径。
     * 由 [DownloadWorker] 在传输成功完成时调用。
     */
    suspend fun updateCompleted(downloadId: String, localPath: String)
}
