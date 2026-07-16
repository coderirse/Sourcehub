package com.example.sourcehub.data.remote.mock

import com.example.sourcehub.data.local.mock.MockDataProvider
import com.example.sourcehub.data.remote.api.ProductApi
import com.example.sourcehub.data.remote.dto.*
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * [ProductApi] 的内存模拟实现，用于开发和测试。
 *
 * ## 数据源
 * 所有数据从 [MockDataProvider] 读取，其中包含 20 个商品、6 个分类和 3 个横幅的静态目录。
 *
 * ## 各端点的模拟行为
 *
 * - **[getBanners]**: 返回模拟数据中的所有横幅。延迟: 200-600 毫秒。
 * - **[getCategories]**: 返回所有分类。延迟: 200-500 毫秒。
 * - **[getRecommendedProducts]**: 按 [salesCount] 降序排列商品，
 *   取前 [limit] 条。延迟: 300-700 毫秒。
 * - **[getNewArrivals]**: 按 [createdAt] 降序排列商品，
 *   取前 [limit] 条。延迟: 300-700 毫秒。
 * - **[getProductsByCategory]**: 按 [categoryId] 筛选商品。
 *   如果没有匹配的商品则返回 404。延迟: 300-600 毫秒。
 * - **[getProductDetail]**: 通过 ID 查找单个商品。
 *   未找到则返回 404。延迟: 300-700 毫秒。
 * - **[searchProducts]**: 对标题、作者、描述和标签进行不区分大小写的匹配。
 *   延迟: 400-900 毫秒。
 *
 * ## 无错误模拟
 * 与 [MockAuthApi] 和 [MockPaymentApi] 不同，此模拟不注入
 * 人为故障。只要数据存在，所有调用都会成功。
 */
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
        // 按销量降序排列，最受欢迎的商品排在最前。
        val recommended = mockData.products.sortedByDescending { it.salesCount }.take(limit)
        return ApiResponse(data = recommended.map { it.toResponse() })
    }

    override suspend fun getNewArrivals(limit: Int): ApiResponse<List<ProductResponse>> {
        delay(Random.nextLong(300, 700))
        // 按创建时间降序排列，最新商品排在最前。
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

    /**
     * 将领域 [Product] 转换为 DTO [ProductResponse]。
     * [FileType] 枚举按名称序列化为传输格式。
     */
    private fun com.example.sourcehub.domain.model.Product.toResponse() = ProductResponse(
        id = id, title = title, description = description, author = author,
        price = price, originalPrice = originalPrice, coverUrl = coverUrl,
        fileUrl = fileUrl, fileType = fileType.name, pageCount = pageCount,
        fileSize = fileSize, categoryId = categoryId, salesCount = salesCount,
        rating = rating, isPublished = isPublished, tags = tags, createdAt = createdAt
    )
}
