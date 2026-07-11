package com.example.sourcehub.data.remote.mock

import com.example.sourcehub.data.local.mock.MockDataProvider
import com.example.sourcehub.data.remote.api.DownloadApi
import com.example.sourcehub.data.remote.dto.*
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.delay
import kotlin.random.Random

class MockDownloadApi(private val mockData: MockDataProvider) : DownloadApi {

    override suspend fun getDownloadUrl(productId: String): ApiResponse<DownloadUrlResponse> {
        delay(Random.nextLong(300, 700))
        val product = mockData.getProductById(productId)
        return if (product != null) {
            ApiResponse(
                data = DownloadUrlResponse(
                    productId = product.id,
                    downloadUrl = "https://mock-download.sourcehub.com/files/${product.id}.${product.fileType.extension}",
                    fileName = "${product.title}.${product.fileType.extension}",
                    fileSize = product.fileSize,
                    expiresAt = System.currentTimeMillis() + 3600000 // 1 hour
                )
            )
        } else {
            ApiResponse(code = 404, message = "商品不存在")
        }
    }

    override suspend fun reportDownloadProgress(request: DownloadProgressRequest): ApiResponse<Unit> {
        delay(Random.nextLong(100, 300))
        return ApiResponse(data = Unit)
    }
}
