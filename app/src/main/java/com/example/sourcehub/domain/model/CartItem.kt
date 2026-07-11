package com.example.sourcehub.domain.model

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
