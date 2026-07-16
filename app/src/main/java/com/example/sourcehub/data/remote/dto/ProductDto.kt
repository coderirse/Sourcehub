package com.example.sourcehub.data.remote.dto

/**
 * 商品目录领域的传输对象。
 */

/**
 * [com.example.sourcehub.domain.model.Product] 的传输格式表示。
 *
 * 字段与领域模型对应，但枚举类型使用 [String]
 * （[fileType]），因为 JSON 传输格式使用枚举名称。
 */
data class ProductResponse(
    val id: String,
    val title: String,
    val description: String,
    val author: String,
    val price: Double,
    val originalPrice: Double,
    val coverUrl: String,
    val fileUrl: String,
    val fileType: String,   // 序列化为 FileType 枚举名称（例如 "PDF"）
    val pageCount: Int,
    val fileSize: Long,
    val categoryId: String,
    val salesCount: Int,
    val rating: Float,
    val isPublished: Boolean,
    val tags: List<String>,
    val createdAt: Long
)

/**
 * [com.example.sourcehub.domain.model.Banner] 的传输格式表示。
 *
 * [linkType] 是 [BannerLinkType] 枚举名称的字符串形式。
 */
data class BannerResponse(
    val id: String,
    val title: String,
    val imageUrl: String,
    val linkType: String,   // 序列化为 BannerLinkType 枚举名称（例如 "PRODUCT"）
    val linkValue: String,
    val sortOrder: Int
)

/**
 * [com.example.sourcehub.domain.model.Category] 的传输格式表示。
 */
data class CategoryResponse(
    val id: String,
    val name: String,
    val iconName: String,
    val sortOrder: Int,
    val productCount: Int
)
