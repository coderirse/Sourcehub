package com.example.sourcehub.data.remote.mock

import com.example.sourcehub.data.local.mock.MockDataProvider
import com.example.sourcehub.data.remote.api.DownloadApi
import com.example.sourcehub.data.remote.dto.*
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * [DownloadApi] 的内存模拟实现，用于开发和测试。
 *
 * ## 模拟行为
 *
 * **getDownloadUrl**:
 * - 通过 ID 在 [MockDataProvider] 中查找商品。
 * - 使用模拟域名构造下载 URL：
 *   `https://mock-download.sourcehub.com/files/{productId}.{extension}`
 * - 将下载 URL 的过期时间设置为从现在起 1 小时。
 * - 未找到商品时返回 404。
 * - 延迟: 300-700 毫秒。
 *
 * **reportDownloadProgress**:
 * - 始终返回成功（空操作）。模拟不跟踪服务端进度；
 *   进度由 [DownloadWorker] 和 [DownloadRepositoryImpl] 在本地管理。
 * - 延迟: 100-300 毫秒。
 */
class MockDownloadApi(private val mockData: MockDataProvider) : DownloadApi {

    override suspend fun getDownloadUrl(productId: String): ApiResponse<DownloadUrlResponse> {
        delay(Random.nextLong(300, 700))
        val product = mockData.getProductById(productId)
        return if (product != null) {
            ApiResponse(
                data = DownloadUrlResponse(
                    productId = product.id,
                    // 使用商品 ID 和文件扩展名构造模拟下载 URL。
                    downloadUrl = "https://mock-download.sourcehub.com/files/${product.id}.${product.fileType.extension}",
                    fileName = "${product.title}.${product.fileType.extension}",
                    fileSize = product.fileSize,
                    expiresAt = System.currentTimeMillis() + 3600000 // 1 小时后过期
                )
            )
        } else {
            ApiResponse(code = 404, message = "商品不存在")
        }
    }

    override suspend fun reportDownloadProgress(request: DownloadProgressRequest): ApiResponse<Unit> {
        delay(Random.nextLong(100, 300))
        // 进度上报在模拟中为空操作 — 真正的跟踪由
        // DownloadWorker 和 DownloadRepositoryImpl 在本地完成。
        return ApiResponse(data = Unit)
    }
}
