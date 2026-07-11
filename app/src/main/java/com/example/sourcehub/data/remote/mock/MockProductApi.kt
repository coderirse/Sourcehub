package com.example.sourcehub.data.remote.mock

import com.example.sourcehub.data.local.mock.MockDataProvider
import com.example.sourcehub.data.remote.api.ProductApi
import com.example.sourcehub.data.remote.dto.*
import kotlinx.coroutines.delay
import kotlin.random.Random

class MockProductApi(private val mockData: MockDataProvider) : ProductApi {

    override suspend fun getBanners(): ApiResponse<List<BannerResponse>> {
        delay(Random.nextLong(200, 600))
        return ApiResponse(
            data = mockData.banners.map { b ->
                BannerResponse(b.id, b.title, b.imageUrl, b.linkType.name, b.linkValue, b.sortOrder)
            }
        )
    }

    override suspend fun getCategories(): ApiResponse<List<CategoryResponse>> {
        delay(Random.nextLong(200, 500))
        return ApiResponse(
            data = mockData.categories.map { c ->
                CategoryResponse(c.id, c.name, c.iconName, c.sortOrder, c.productCount)
            }
        )
    }

    override suspend fun getRecommendedProducts(limit: Int): ApiResponse<List<ProductResponse>> {
        delay(Random.nextLong(300, 700))
        val recommended = mockData.products.sortedByDescending { it.salesCount }.take(limit)
        return ApiResponse(data = recommended.map { it.toResponse() })
    }

    override suspend fun getNewArrivals(limit: Int): ApiResponse<List<ProductResponse>> {
        delay(Random.nextLong(300, 700))
        val newest = mockData.products.sortedByDescending { it.createdAt }.take(limit)
        return ApiResponse(data = newest.map { it.toResponse() })
    }

    override suspend fun getProductsByCategory(categoryId: String): ApiResponse<List<ProductResponse>> {
        delay(Random.nextLong(300, 600))
        val products = mockData.getProductsByCategory(categoryId)
        return ApiResponse(data = products.map { it.toResponse() })
    }

    override suspend fun getProductDetail(productId: String): ApiResponse<ProductResponse> {
        delay(Random.nextLong(300, 700))
        val product = mockData.getProductById(productId)
        return if (product != null) {
            ApiResponse(data = product.toResponse())
        } else {
            ApiResponse(code = 404, message = "商品不存在")
        }
    }

    override suspend fun searchProducts(query: String): ApiResponse<List<ProductResponse>> {
        delay(Random.nextLong(400, 900))
        val results = mockData.searchProducts(query)
        return ApiResponse(data = results.map { it.toResponse() })
    }

    private fun com.example.sourcehub.domain.model.Product.toResponse() = ProductResponse(
        id = id, title = title, description = description, author = author,
        price = price, originalPrice = originalPrice, coverUrl = coverUrl,
        fileUrl = fileUrl, fileType = fileType.name, pageCount = pageCount,
        fileSize = fileSize, categoryId = categoryId, salesCount = salesCount,
        rating = rating, isPublished = isPublished, tags = tags, createdAt = createdAt
    )
}
