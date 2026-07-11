package com.example.sourcehub.data.repository

import com.example.sourcehub.data.remote.api.ProductApi
import com.example.sourcehub.data.remote.dto.BannerResponse
import com.example.sourcehub.data.remote.dto.CategoryResponse
import com.example.sourcehub.data.remote.dto.ProductResponse
import com.example.sourcehub.domain.model.*
import com.example.sourcehub.domain.repository.ProductRepository
import com.example.sourcehub.presentation.common.state.Resource

class ProductRepositoryImpl(
    private val productApi: ProductApi
) : ProductRepository {

    override suspend fun getBanners(): Resource<List<Banner>> = apiCall(
        call = { productApi.getBanners() },
        map = { list -> list.map { it.toDomain() } }
    )

    override suspend fun getCategories(): Resource<List<Category>> = apiCall(
        call = { productApi.getCategories() },
        map = { list -> list.map { it.toDomain() } }
    )

    override suspend fun getRecommendedProducts(limit: Int): Resource<List<Product>> = apiCall(
        call = { productApi.getRecommendedProducts(limit) },
        map = { list -> list.map { it.toDomain() } }
    )

    override suspend fun getNewArrivals(limit: Int): Resource<List<Product>> = apiCall(
        call = { productApi.getNewArrivals(limit) },
        map = { list -> list.map { it.toDomain() } }
    )

    override suspend fun getProductsByCategory(categoryId: String): Resource<List<Product>> = apiCall(
        call = { productApi.getProductsByCategory(categoryId) },
        map = { list -> list.map { it.toDomain() } }
    )

    override suspend fun getProductDetail(productId: String): Resource<Product> {
        return try {
            val r = productApi.getProductDetail(productId)
            if (r.code == 200 && r.data != null) Resource.Success(r.data.toDomain())
            else Resource.Error(r.message)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "加载失败")
        }
    }

    override suspend fun searchProducts(query: String): Resource<List<Product>> = apiCall(
        call = { productApi.searchProducts(query) },
        map = { list -> list.map { it.toDomain() } }
    )

    private suspend fun <T, R> apiCall(
        call: suspend () -> com.example.sourcehub.data.remote.dto.ApiResponse<T>,
        map: (T) -> R
    ): Resource<R> {
        return try {
            val r = call()
            if (r.code == 200 && r.data != null) Resource.Success(map(r.data))
            else Resource.Error(r.message)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
        }
    }

    private fun ProductResponse.toDomain() = Product(
        id, title, description, author, price, originalPrice, coverUrl, fileUrl,
        try { FileType.valueOf(fileType) } catch (e: Exception) { FileType.PDF },
        pageCount, fileSize, categoryId, salesCount, rating, isPublished, tags, createdAt
    )

    private fun BannerResponse.toDomain() = Banner(
        id, title, imageUrl,
        try { BannerLinkType.valueOf(linkType) } catch (e: Exception) { BannerLinkType.NONE },
        linkValue, sortOrder
    )

    private fun CategoryResponse.toDomain() = Category(id, name, iconName, sortOrder, productCount)
}
