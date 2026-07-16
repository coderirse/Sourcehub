package com.example.sourcehub.domain.model

/**
 * 表示用户购物车中一项的领域模型。
 *
 * 购物车项持久化在 SQLite数据库中，并通过[CartRepository]
 * 响应式地展示。当用户添加的商品已在购物车中时，
 * 数量会递增而不是创建重复行。
 *
 * @property id 唯一购物车项标识符（例如 "cart_a1b2c3d4"）。
 * @property userId 拥有此购物车的用户。
 * @property productId 添加到购物车的商品。
 * @property productTitle 用于离线展示的商品标题快照。
 * @property productCover 用于离线展示的商品封面图片 URL 快照。
 * @property price 添加项时的单价。
 * @property quantity 数量（始终 >= 1；减至 0 会触发移除）。
 * @property addedAt 项首次添加时的毫秒时间戳。
 */
data class CartItem(
    val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val productCover: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val addedAt: Long = System.currentTimeMillis()
)
