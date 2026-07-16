package com.example.sourcehub.domain.repository

import com.example.sourcehub.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

/**
 * 购物车操作的契约接口。
 *
 * 购物车商品项持久化存储在 SQLite数据库 中，并以响应式方式暴露，
 * 使购物车角标数量和商品列表能够自动更新。添加已存在的商品项
 * 会增加其数量，而不是创建重复项。
 * 将数量设为 0 则会移除该商品项。
 */
interface CartRepository {

    /**
     * 将商品添加到购物车，如果已存在则增加数量。
     *
     * @param userId 添加商品的用户。
     * @param productId 要添加的商品。
     * @param productTitle 商品标题快照，用于离线显示。
     * @param productCover 商品封面快照，用于离线显示。
     * @param price 添加时的单价快照。
     */
    suspend fun addToCart(userId: String, productId: String, productTitle: String, productCover: String, price: Double)

    /** 通过行 ID 移除单个购物车商品项。 */
    suspend fun removeFromCart(itemId: String)

    /**
     * 更新购物车商品项的数量。
     * 如果 [quantity] <= 0，则完全移除该商品项。
     */
    suspend fun updateQuantity(itemId: String, quantity: Int)

    /** 移除用户的所有购物车商品项。 */
    suspend fun clearCart(userId: String)

    /**
     * 以响应式方式观察用户的购物车商品项列表。
     * 商品项按 userId 过滤，并按 addedAt 降序排列。
     */
    fun getCartItems(userId: String): Flow<List<CartItem>>

    /**
     * 观察用户购物车中不同商品项的总数。
     * 用于底部导航栏中的购物车角标。
     */
    fun getCartCount(userId: String): Flow<Int>

    /**
     * 按用户和商品查找单个购物车商品项，若不存在则返回 null。
     * 用于检查某个商品是否已在购物车中。
     */
    suspend fun getCartItem(userId: String, productId: String): CartItem?
}
