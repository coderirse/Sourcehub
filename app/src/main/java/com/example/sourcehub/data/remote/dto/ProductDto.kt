package com.example.sourcehub.data.remote.dto

data class ProductResponse(
    val id: String,
    val title: String,
    val description: String,
    val author: String,
    val price: Double,
    val originalPrice: Double,
    val coverUrl: String,
    val fileUrl: String,
    val fileType: String,
    val pageCount: Int,
    val fileSize: Long,
    val categoryId: String,
    val salesCount: Int,
    val rating: Float,
    val isPublished: Boolean,
    val tags: List<String>,
    val createdAt: Long
)

data class BannerResponse(
    val id: String,
    val title: String,
    val imageUrl: String,
    val linkType: String,
    val linkValue: String,
    val sortOrder: Int
)

data class CategoryResponse(
    val id: String,
    val name: String,
    val iconName: String,
    val sortOrder: Int,
    val productCount: Int
)
