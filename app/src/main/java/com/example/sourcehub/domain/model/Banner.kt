package com.example.sourcehub.domain.model

/**
 * 表示首页轮播中推广横幅的领域模型。
 *
 * 横幅用于精选内容、促销活动以及分类/商品的深度链接。
 * 每个横幅指定一个链接动作，当用户点击时触发。
 *
 * @property id 唯一横幅标识符（例如 "b1"）。
 * @property title 以浮层或标题形式显示的展示标题。
 * @property imageUrl 横幅图片的远程 URL（理想尺寸 800x300）。
 * @property linkType 点击时触发的导航动作类型。
 * @property linkValue 链接的目标值（商品 ID、分类 ID 或 URL）。
 * @property sortOrder 决定轮播显示顺序（值越小越靠前）。
 */
data class Banner(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val linkType: BannerLinkType = BannerLinkType.NONE,
    val linkValue: String = "",
    val sortOrder: Int = 0
)

/**
 * 横幅点击动作的导航目标类型。
 *
 * - [PRODUCT]：导航到商品详情页面；[Banner.linkValue] 是商品 ID。
 * - [CATEGORY]：导航到分类列表；[Banner.linkValue] 是分类 ID。
 * - [URL]：打开外部 URL；[Banner.linkValue] 是一个完整 URL。
 * - [NONE]：无动作；横幅仅用于展示。
 */
enum class BannerLinkType {
    PRODUCT, CATEGORY, URL, NONE
}
