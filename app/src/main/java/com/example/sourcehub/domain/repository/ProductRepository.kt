package com.example.sourcehub.domain.repository

import com.example.sourcehub.domain.model.Banner
import com.example.sourcehub.domain.model.Category
import com.example.sourcehub.domain.model.Product
import com.example.sourcehub.presentation.common.state.Resource

interface ProductRepository {
    suspend fun getBanners(): Resource<List<Banner>>
    suspend fun getCategories(): Resource<List<Category>>
    suspend fun getRecommendedProducts(limit: Int = 10): Resource<List<Product>>
    suspend fun getNewArrivals(limit: Int = 10): Resource<List<Product>>
    suspend fun getProductsByCategory(categoryId: String): Resource<List<Product>>
    suspend fun getProductDetail(productId: String): Resource<Product>
    suspend fun searchProducts(query: String): Resource<List<Product>>
}
