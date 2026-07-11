package com.example.sourcehub.data.remote.api

import com.example.sourcehub.data.remote.dto.*

interface DownloadApi {
    suspend fun getDownloadUrl(productId: String): ApiResponse<DownloadUrlResponse>
    suspend fun reportDownloadProgress(request: DownloadProgressRequest): ApiResponse<Unit>
}
