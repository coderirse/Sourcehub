package com.example.sourcehub.data.remote.api

import com.example.sourcehub.data.remote.dto.*

interface ProductApi {
    suspend fun getBanners(): ApiResponse<List<BannerResponse>>
    suspend fun getCategories(): ApiResponse<List<CategoryResponse>>
    suspend fun getRecommendedProducts(limit: Int = 10): ApiResponse<List<ProductResponse>>
    suspend fun getNewArrivals(limit: Int = 10): ApiResponse<List<ProductResponse>>
    suspend fun getProductsByCategory(categoryId: String): ApiResponse<List<ProductResponse>>
    suspend fun getProductDetail(productId: String): ApiResponse<ProductResponse>
    suspend fun searchProducts(query: String): ApiResponse<List<ProductResponse>>
}
