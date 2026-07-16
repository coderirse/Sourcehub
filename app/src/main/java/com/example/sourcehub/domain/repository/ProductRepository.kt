package com.example.sourcehub.domain.repository

import com.example.sourcehub.domain.model.Banner
import com.example.sourcehub.domain.model.Category
import com.example.sourcehub.domain.model.Product
import com.example.sourcehub.presentation.common.state.Resource

/**
 * 商品目录操作的契约接口。
 *
 * 提供对商品目录的只读访问：横幅广告、分类、
 * 不同排序方式下的商品列表、详情视图和搜索。
 * 所有方法均返回 [Resource] 包装器，以便调用方可以
 * 统一处理加载中、成功和错误状态。
 */
interface ProductRepository {

    /** 获取用于首页轮播的促销横幅广告。 */
    suspend fun getBanners(): Resource<List<Banner>>

    /** 获取包含商品数量的完整商品分类列表。 */
    suspend fun getCategories(): Resource<List<Category>>

    /**
     * 获取推荐商品，按 [Product.salesCount] 降序排列。
     * @param limit 返回商品的最大数量（默认 10）。
     */
    suspend fun getRecommendedProducts(limit: Int = 10): Resource<List<Product>>

    /**
     * 获取最近上架的商品，按 [Product.createdAt] 降序排列。
     * @param limit 返回商品的最大数量（默认 10）。
     */
    suspend fun getNewArrivals(limit: Int = 10): Resource<List<Product>>

    /**
     * 获取属于指定分类的所有已发布商品。
     * @param categoryId 父分类 ID。
     */
    suspend fun getProductsByCategory(categoryId: String): Resource<List<Product>>

    /**
     * 获取单个商品的完整详情。
     * @param productId 要查找的商品 ID。
     */
    suspend fun getProductDetail(productId: String): Resource<Product>

    /**
     * 在商品标题、作者、描述和标签中进行全文搜索。
     * @param query 搜索关键词。
     */
    suspend fun searchProducts(query: String): Resource<List<Product>>
}
