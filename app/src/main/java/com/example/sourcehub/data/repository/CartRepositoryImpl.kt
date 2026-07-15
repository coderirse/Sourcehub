package com.example.sourcehub.data.repository

import com.example.sourcehub.data.local.db.SourcehubDbHelper
import com.example.sourcehub.domain.model.CartItem
import com.example.sourcehub.domain.repository.CartRepository
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class CartRepositoryImpl(private val db: SourcehubDbHelper) : CartRepository {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())

    init {
        kotlinx.coroutines.runBlocking {
            _items.value = db.getCartItems("") // Load all; filtered by userId in getCartItems()
        }
    }

    private fun emitFromDb(userId: String) {
        kotlinx.coroutines.runBlocking {
            _items.value = db.getCartItems("") // Full reload for cross-user compatibility
        }
    }

    override suspend fun addToCart(userId: String, productId: String, productTitle: String, productCover: String, price: Double) {
        val existing = db.getCartItem(userId, productId)
        if (existing != null) {
            db.updateCartQuantity(existing.id, existing.quantity + 1)
        } else {
            db.insertCartItem(CartItem("cart_${SecurityUtils.generateUuid().take(8)}", userId, productId, productTitle, productCover, price))
        }
        emitFromDb(userId)
    }

    override suspend fun removeFromCart(itemId: String) { db.deleteCartItem(itemId); emitFromDb("") }
    override suspend fun updateQuantity(itemId: String, quantity: Int) {
        if (quantity > 0) db.updateCartQuantity(itemId, quantity)
        else db.deleteCartItem(itemId)
        emitFromDb("")
    }
    override suspend fun clearCart(userId: String) { db.clearCart(userId); emitFromDb(userId) }
    override fun getCartItems(userId: String): Flow<List<CartItem>> = _items.map { it.filter { item -> item.userId == userId } }
    override fun getCartCount(userId: String): Flow<Int> {
        return _items.map { it.count { item -> item.userId == userId } }
    }
    override suspend fun getCartItem(userId: String, productId: String): CartItem? = db.getCartItem(userId, productId)
}
