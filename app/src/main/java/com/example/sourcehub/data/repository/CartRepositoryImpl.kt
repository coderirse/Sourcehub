package com.example.sourcehub.data.repository

import com.example.sourcehub.data.local.db.SourcehubDbHelper
import com.example.sourcehub.domain.model.CartItem
import com.example.sourcehub.domain.repository.CartRepository
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * 基于 SQLite数据库 的 [CartRepository] 实现。
 *
 * ## 架构
 * 购物车商品存储在由 [SourcehubDbHelper] 管理的 `cart_items` 表中。
 * 内存中的 [MutableStateFlow] 镜像表内容，以便
 * [getCartItems] 和 [getCartCount] 可以返回响应式 [Flow]。
 *
 * ## 去重
 * 当对已在购物车中的商品调用 [addToCart] 时，现有
 * 商品的数量会增加，而不是插入重复行。
 *
 * ## 数量下限
 * [updateQuantity] 的值 <= 0 时会移除该商品。这与
 * 购物车界面步进器使用的约定一致。
 */
class CartRepositoryImpl(private val db: SourcehubDbHelper) : CartRepository {
    /** `cart_items` 表的内存镜像，用于响应式观察。 */
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())

    init {
        // 构造时加载已持久化的购物车商品。
        kotlinx.coroutines.runBlocking {
            _items.value = db.getCartItems("") // 加载全部；在 getCartItems() 中按 userId 过滤
        }
    }

    /** 在变更后从 SQLite数据库 重新加载内存流。 */
    private fun emitFromDb(userId: String) {
        kotlinx.coroutines.runBlocking {
            _items.value = db.getCartItems("") // 全量重新加载以保证跨用户兼容性
        }
    }

    override suspend fun addToCart(userId: String, productId: String, productTitle: String, productCover: String, price: Double) {
        val existing = db.getCartItem(userId, productId)
        if (existing != null) {
            // 商品已在购物车中 — 增加数量而不是创建重复项。
            db.updateCartQuantity(existing.id, existing.quantity + 1)
        } else {
            // 生成唯一的购物车商品 ID 并插入新行。
            db.insertCartItem(CartItem("cart_${SecurityUtils.generateUuid().take(8)}", userId, productId, productTitle, productCover, price))
        }
        emitFromDb(userId)
    }

    override suspend fun removeFromCart(itemId: String) { db.deleteCartItem(itemId); emitFromDb("") }
    override suspend fun updateQuantity(itemId: String, quantity: Int) {
        if (quantity > 0) db.updateCartQuantity(itemId, quantity)
        else db.deleteCartItem(itemId) // 零或负数数量会移除该商品。
        emitFromDb("")
    }
    override suspend fun clearCart(userId: String) { db.clearCart(userId); emitFromDb(userId) }

    /** 按调用者的用户 ID 过滤完整的内存列表。 */
    override fun getCartItems(userId: String): Flow<List<CartItem>> =
        _items.map { it.filter { item -> item.userId == userId } }

    /** 统计不同商品数量（而非总数量）用于角标显示。 */
    override fun getCartCount(userId: String): Flow<Int> {
        return _items.map { it.count { item -> item.userId == userId } }
    }

    override suspend fun getCartItem(userId: String, productId: String): CartItem? = db.getCartItem(userId, productId)
}
