package com.example.sourcehub.data.remote.api

import com.example.sourcehub.data.remote.dto.*

/**
 * 商品目录的 API 契约。
 *
 * 实现: [MockProductApi]（内存模拟数据）和
 * [RetrofitProductApi]（通过 Retrofit 调用 Ktor 后端）。
 */
interface ProductApi {
    /** 获取首页轮播的推广横幅。 */
    suspend fun getBanners(): ApiResponse<List<BannerResponse>>

    /** 获取所有商品分类及数量。 */
    suspend fun getCategories(): ApiResponse<List<CategoryResponse>>

    /** 获取推荐（畅销）商品，限制为 [limit] 条。 */
    suspend fun getRecommendedProducts(limit: Int = 10): ApiResponse<List<ProductResponse>>

    /** 获取最新添加的商品，限制为 [limit] 条。 */
    suspend fun getNewArrivals(limit: Int = 10): ApiResponse<List<ProductResponse>>

    /** 获取按 [categoryId] 筛选的商品。 */
    suspend fun getProductsByCategory(categoryId: String): ApiResponse<List<ProductResponse>>

    /** 通过 [productId] 获取单个商品的完整详情。 */
    suspend fun getProductDetail(productId: String): ApiResponse<ProductResponse>

    /** 搜索标题、作者、描述和标签。 */
    suspend fun searchProducts(query: String): ApiResponse<List<ProductResponse>>
}
