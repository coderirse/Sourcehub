package com.example.sourcehub.data.remote.dto

data class CreateOrderRequest(
    val userId: String,
    val items: List<OrderItemRequest>,
    val couponCode: String = ""
)

data class OrderItemRequest(
    val productId: String,
    val quantity: Int
)

data class OrderResponse(
    val id: String,
    val userId: String,
    val itemsJson: String,
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
