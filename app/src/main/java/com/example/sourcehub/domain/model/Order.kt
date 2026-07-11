package com.example.sourcehub.domain.model

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

data class OrderItem(
    val id: String = "",
    val orderId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val productCover: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 1
)

enum class OrderStatus(val label: String) {
    PENDING("待支付"),
    PAID("已支付"),
    CANCELLED("已取消"),
    REFUNDED("已退款")
}

enum class PaymentMethod(val label: String) {
    WECHAT("微信支付"),
    ALIPAY("支付宝"),
    CREDIT_CARD("信用卡")
}
