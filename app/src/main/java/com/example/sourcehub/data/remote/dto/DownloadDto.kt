package com.example.sourcehub.data.remote.dto

/**
 * 文件下载领域的传输对象。
 */

/**
 * 下载 URL 端点的响应。
 *
 * @property productId 已购买的商品。
 * @property downloadUrl 用于下载文件的预签名（或直接）URL。
 * @property fileName 建议的文件名，包含扩展名。
 * @property fileSize 预期文件大小（字节）。
 * @property expiresAt 下载 URL 过期时间戳（毫秒）（签发后 1 小时）。
 */
data class DownloadUrlResponse(
    val productId: String,
    val downloadUrl: String,
    val fileName: String,
    val fileSize: Long,
    val expiresAt: Long
)

/**
 * 向后端上报下载进度的请求体。
 *
 * 注意: 模拟实现将此视为空操作。
 * 真正的进度跟踪由 [com.example.sourcehub.worker.DownloadWorker] 在本地完成。
 *
 * @property downloadId 正在上报的下载任务。
 * @property progress 完成比例 (0.0 - 1.0)。
 * @property downloadedBytes 累计接收的字节数。
 */
data class DownloadProgressRequest(
    val downloadId: String,
    val progress: Float,
    val downloadedBytes: Long
)
