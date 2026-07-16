package com.example.sourcehub.domain.model

/**
 * 表示客户订单的领域模型。
 *
 * 当用户从购物车进行结算时创建一个订单。它记录购买的项列表、
 * 定价详情（包括任何优惠券折扣）、支付状态以及选择的支付方式。
 *
 * @property id 唯一订单标识符（例如 "order_a1b2c3d4"）。
 * @property userId 下此订单的用户。
 * @property items 此订单中包含的行项列表。
 * @property totalAmount 折扣前所有项的（单价 * 数量）之和。
 * @property discountAmount 应用到此订单的优惠券或促销折扣。
 * @property finalAmount 实际应付金额（= totalAmount - discountAmount）。
 * @property status 订单的当前生命周期状态。
 * @property paymentMethod 结算时选择的支付渠道。
 * @property transactionId 支付网关交易参考号，支付成功后填充。
 * @property couponCode 应用的优惠券代码（如有，例如 "SAVE10"）。
 * @property createdAt 订单创建的毫秒时间戳。
 * @property paidAt 支付成功的毫秒时间戳；未支付则为 0。
 */
data class Order(
    val id: String = "",
    val userId: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentMethod: PaymentMethod = PaymentMethod.WECHAT,
    val transactionId: String = "",
    val couponCode: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val paidAt: Long = 0L
)

/**
 * [Order]中的单个行项。
 *
 * @property id 唯一项标识符。
 * @property orderId 此项所属的父订单。
 * @property productId 购买的商品。
 * @property productTitle 购买时商品标题的快照。
 * @property productCover 购买时商品封面图片 URL 的快照。
 * @property unitPrice 购买时的单价。
 * @property quantity 购买数量（对于数字商品几乎总是 1，但保留灵活性）。
 */
data class OrderItem(
    val id: String = "",
    val orderId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val productCover: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 1
)

/**
 * [Order]的生命周期状态。
 *
 * @property label 用于界面渲染的中文本地化显示标签。
 */
enum class OrderStatus(val label: String) {
    PENDING("待支付"),
    PAID("已支付"),
    CANCELLED("已取消"),
    REFUNDED("已退款")
}

/**
 * 结算时支持的支付渠道。
 *
 * @property label 用于界面渲染的中文本地化显示标签。
 */
enum class PaymentMethod(val label: String) {
    WECHAT("微信支付"),
    ALIPAY("支付宝"),
    CREDIT_CARD("信用卡")
}
