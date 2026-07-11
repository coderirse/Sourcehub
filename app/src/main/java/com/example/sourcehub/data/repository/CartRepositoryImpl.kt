package com.example.sourcehub.data.repository

import com.example.sourcehub.domain.model.CartItem
import com.example.sourcehub.domain.repository.CartRepository
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class CartRepositoryImpl : CartRepository {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())

    override suspend fun addToCart(
        userId: String, productId: String, productTitle: String, productCover: String, price: Double
    ) {
        val existing = _items.value.find { it.userId == userId && it.productId == productId }
        if (existing != null) {
            _items.value = _items.value.map {
                if (it.id == existing.id) it.copy(quantity = it.quantity + 1) else it
            }
        } else {
            val item = CartItem(
                id = "cart_${SecurityUtils.generateUuid().take(8)}",
                userId = userId, productId = productId,
                productTitle = productTitle, productCover = productCover,
                price = price, quantity = 1
            )
            _items.value = _items.value + item
        }
    }

    override suspend fun removeFromCart(itemId: String) {
        _items.value = _items.value.filter { it.id != itemId }
    }

    override suspend fun updateQuantity(itemId: String, quantity: Int) {
        if (quantity > 0) {
            _items.value = _items.value.map {
                if (it.id == itemId) it.copy(quantity = quantity) else it
            }
        } else {
            _items.value = _items.value.filter { it.id != itemId }
        }
    }

    override suspend fun clearCart(userId: String) {
        _items.value = _items.value.filter { it.userId != userId }
    }

    override fun getCartItems(userId: String): Flow<List<CartItem>> {
        return _items.map { it.filter { item -> item.userId == userId } }
    }

    override fun getCartCount(userId: String): Flow<Int> {
        return _items.map { it.count { item -> item.userId == userId } }
    }

    override suspend fun getCartItem(userId: String, productId: String): CartItem? {
        return _items.value.find { it.userId == userId && it.productId == productId }
    }
}
