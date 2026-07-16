package com.example.sourcehub.data.remote.api

import com.example.sourcehub.data.remote.dto.*

/**
 * 文件下载操作的 API 契约。
 *
 * 实现: [MockDownloadApi]（返回模拟 URL）和
 * [RetrofitDownloadApi]（通过 Retrofit 调用 Ktor 后端）。
 */
interface DownloadApi {
    /** 获取已购买商品的预签名（或直接）下载 URL。 */
    suspend fun getDownloadUrl(productId: String): ApiResponse<DownloadUrlResponse>

    /**
     * 向后端上报下载进度。
     *
     * 注意: 在模拟实现中这是一个空操作。
     * 真正的进度跟踪由 [com.example.sourcehub.worker.DownloadWorker] 在本地完成。
     */
    suspend fun reportDownloadProgress(request: DownloadProgressRequest): ApiResponse<Unit>
}
