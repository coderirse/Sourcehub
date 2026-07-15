package com.example.sourcehub.data.repository

import com.example.sourcehub.data.local.persistence.JsonPersistenceManager
import com.example.sourcehub.data.local.persistence.toCartItem
import com.example.sourcehub.data.local.persistence.toJson
import com.example.sourcehub.domain.model.CartItem
import com.example.sourcehub.domain.repository.CartRepository
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class CartRepositoryImpl(private val persistence: JsonPersistenceManager) : CartRepository {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())

    init {
        // Load from persisted JSON on init
        kotlinx.coroutines.runBlocking {
            val arr = persistence.loadArray("cart_items")
            if (arr != null) {
                val loaded = mutableListOf<CartItem>()
                for (i in 0 until arr.length()) {
                    loaded.add(arr.getJSONObject(i).toCartItem())
                }
                _items.value = loaded
            }
        }
    }

    private suspend fun persist() {
        val arr = JSONArray()
        _items.value.forEach { arr.put(it.toJson()) }
        persistence.saveArray("cart_items", arr)
    }

    override suspend fun addToCart(userId: String, productId: String, productTitle: String, productCover: String, price: Double) {
        val existing = _items.value.find { it.userId == userId && it.productId == productId }
        if (existing != null) {
            _items.value = _items.value.map { if (it.id == existing.id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            _items.value = _items.value + CartItem("cart_${SecurityUtils.generateUuid().take(8)}", userId, productId, productTitle, productCover, price)
        }
        persist()
    }

    override suspend fun removeFromCart(itemId: String) { _items.value = _items.value.filter { it.id != itemId }; persist() }
    override suspend fun updateQuantity(itemId: String, quantity: Int) {
        if (quantity > 0) _items.value = _items.value.map { if (it.id == itemId) it.copy(quantity = quantity) else it }
        else _items.value = _items.value.filter { it.id != itemId }
        persist()
    }
    override suspend fun clearCart(userId: String) { _items.value = _items.value.filter { it.userId != userId }; persist() }
    override fun getCartItems(userId: String): Flow<List<CartItem>> = _items.map { it.filter { item -> item.userId == userId } }
    override fun getCartCount(userId: String): Flow<Int> = _items.map { it.count { item -> item.userId == userId } }
    override suspend fun getCartItem(userId: String, productId: String): CartItem? = _items.value.find { it.userId == userId && it.productId == productId }
}
