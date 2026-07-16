package com.example.sourcehub.data.repository

import com.example.sourcehub.data.remote.api.ProductApi
import com.example.sourcehub.data.remote.dto.BannerResponse
import com.example.sourcehub.data.remote.dto.CategoryResponse
import com.example.sourcehub.data.remote.dto.ProductResponse
import com.example.sourcehub.domain.model.*
import com.example.sourcehub.domain.repository.ProductRepository
import com.example.sourcehub.presentation.common.state.Resource

/**
 * [ProductRepository] 的实现，委托给 [ProductApi]。
 *
 * ## 架构
 * 每个方法调用对应的 API 端点，检查 HTTP 风格的
 * [ApiResponse.code]（200 = 成功），通过私有扩展函数
 * 将 DTO 列表映射为领域模型，并将结果封装在 [Resource] 中。
 *
 * ## DTO 到领域模型的映射
 * [ProductResponse.toDomain] 将网络格式转换为 [Product]。
 * 枚举字段（[FileType]、[BannerLinkType]）按名称解析；
 * 无法识别的值回退到安全默认值（PDF / NONE）。
 *
 * ## API 切换
 * [swapApi] 允许在模拟和 网络层 实现之间热切换。
 */
class ProductRepositoryImpl(private var productApi: ProductApi) : ProductRepository {
    /** 在运行时替换底层 API（模拟 <-> 网络层）。 */
    fun swapApi(api: ProductApi) { productApi = api }

    override suspend fun getBanners(): Resource<List<Banner>> {
        return try {
            val r = productApi.getBanners()
            if (r.code == 200 && r.data != null) Resource.Success(r.data.map { it.toDomain() })
            else Resource.Error(r.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "网络错误") }
    }

    override suspend fun getCategories(): Resource<List<Category>> {
        return try {
            val r = productApi.getCategories()
            if (r.code == 200 && r.data != null) Resource.Success(r.data.map { it.toDomain() })
            else Resource.Error(r.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "网络错误") }
    }

    override suspend fun getRecommendedProducts(limit: Int): Resource<List<Product>> {
        return try {
            val r = productApi.getRecommendedProducts(limit)
            if (r.code == 200 && r.data != null) Resource.Success(r.data.map { it.toDomain() })
            else Resource.Error(r.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "网络错误") }
    }

    override suspend fun getNewArrivals(limit: Int): Resource<List<Product>> {
        return try {
            val r = productApi.getNewArrivals(limit)
            if (r.code == 200 && r.data != null) Resource.Success(r.data.map { it.toDomain() })
            else Resource.Error(r.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "网络错误") }
    }

    override suspend fun getProductsByCategory(categoryId: String): Resource<List<Product>> {
        return try {
            val r = productApi.getProductsByCategory(categoryId)
            if (r.code == 200 && r.data != null) Resource.Success(r.data.map { it.toDomain() })
            else Resource.Error(r.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "网络错误") }
    }

    override suspend fun getProductDetail(productId: String): Resource<Product> {
        return try {
            val r = productApi.getProductDetail(productId)
            if (r.code == 200 && r.data != null) Resource.Success(r.data.toDomain())
            else Resource.Error(r.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "加载失败") }
    }

    override suspend fun searchProducts(query: String): Resource<List<Product>> {
        return try {
            val r = productApi.searchProducts(query)
            if (r.code == 200 && r.data != null) Resource.Success(r.data.map { it.toDomain() })
            else Resource.Error(r.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "网络错误") }
    }

    /**
     * 将 DTO [ProductResponse] 映射为领域 [Product] 模型。
     * [fileType] 字符串通过 [FileType.valueOf] 解析；
     * 如果服务器发送了未知值，则回退到 [FileType.PDF]。
     */
    private fun ProductResponse.toDomain() = Product(
        id, title, description, author, price, originalPrice, coverUrl, fileUrl,
        try { FileType.valueOf(fileType) } catch (e: Exception) { FileType.PDF },
        pageCount, fileSize, categoryId, salesCount, rating, isPublished, tags, createdAt
    )

    /**
     * 将 DTO [BannerResponse] 映射为领域 [Banner] 模型。
     * [linkType] 字符串通过 [BannerLinkType.valueOf] 解析，
     * 无法识别的值回退到 [BannerLinkType.NONE]。
     */
    private fun BannerResponse.toDomain() = Banner(
        id, title, imageUrl,
        try { BannerLinkType.valueOf(linkType) } catch (e: Exception) { BannerLinkType.NONE },
        linkValue, sortOrder
    )

    /** [CategoryResponse] -> [Category] 的直接 1:1 映射。 */
    private fun CategoryResponse.toDomain() = Category(id, name, iconName, sortOrder, productCount)
}
