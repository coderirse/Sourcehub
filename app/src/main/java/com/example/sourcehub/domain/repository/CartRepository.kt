package com.example.sourcehub.domain.repository

import com.example.sourcehub.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    suspend fun addToCart(userId: String, productId: String, productTitle: String, productCover: String, price: Double)
    suspend fun removeFromCart(itemId: String)
    suspend fun updateQuantity(itemId: String, quantity: Int)
    suspend fun clearCart(userId: String)
    fun getCartItems(userId: String): Flow<List<CartItem>>
    fun getCartCount(userId: String): Flow<Int>
    suspend fun getCartItem(userId: String, productId: String): CartItem?
}
