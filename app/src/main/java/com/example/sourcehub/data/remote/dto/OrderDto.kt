package com.example.sourcehub.data.remote.dto

/**
 * 订单领域的传输对象。
 */

/**
 * POST /api/orders 的请求体。
 *
 * @property userId 下单用户。
 * @property items 商品/数量对列表。
 * @property couponCode 可选的优惠券码（例如 "SAVE10"）。
 */
data class CreateOrderRequest(
    val userId: String,
    val items: List<OrderItemRequest>,
    val couponCode: String = ""
)

/**
 * [CreateOrderRequest] 中的单个订单项。
 *
 * @property productId 要购买的商品。
 * @property quantity 数量（数字商品通常为 1）。
 */
data class OrderItemRequest(
    val productId: String,
    val quantity: Int
)

/**
 * 订单的传输格式响应。
 *
 * @property id 唯一订单标识符。
 * @property userId 下单用户。
 * @property itemsJson 订单项的 JSON 字符串（传输中不是结构化数组）。
 *   由 [OrderRepositoryImpl.parseItems] 手动解析。
 * @property totalAmount 折扣前总金额。
 * @property discountAmount 已应用的优惠券折扣。
 * @property finalAmount 实际支付金额（= totalAmount - discountAmount）。
 * @property status 订单生命周期状态的字符串形式（例如 "PENDING", "PAID"）。
 * @property paymentMethod 支付渠道的字符串形式（例如 "WECHAT"）。
 * @property transactionId 支付网关交易参考号。
 * @property couponCode 已应用的优惠券码（如有）。
 * @property createdAt 订单创建时间戳（毫秒）。
 * @property paidAt 支付时间戳（毫秒）；未支付时为 0。
 */
data class OrderResponse(
    val id: String,
    val userId: String,
    val itemsJson: String,  // 手工构建的订单项 JSON 数组
    val totalAmount: Double,
    val discountAmount: Double,
    val finalAmount: Double,
    val status: String,
    val paymentMethod: String,
    val transactionId: String,
    val couponCode: String,
    val createdAt: Long,
    val paidAt: Long
)
