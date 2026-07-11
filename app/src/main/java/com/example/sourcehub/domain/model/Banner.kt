package com.example.sourcehub.domain.model

data class Banner(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val linkType: BannerLinkType = BannerLinkType.NONE,
    val linkValue: String = "",
    val sortOrder: Int = 0
)

enum class BannerLinkType {
    PRODUCT, CATEGORY, URL, NONE
}
