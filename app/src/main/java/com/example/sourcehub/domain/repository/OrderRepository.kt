package com.example.sourcehub.domain.repository

import com.example.sourcehub.domain.model.Order
import com.example.sourcehub.presentation.common.state.Resource

/**
 * 订单生命周期操作的契约接口。
 *
 * 订单由购物车中的商品创建，可按用户列出、查看详情
 * 和取消。支付通过 [PaymentRepository] 单独处理。
 */
interface OrderRepository {

    /**
     * 根据商品 ID / 数量对列表创建新订单。
     *
     * @param userId 下单的用户。
     * @param items (productId, quantity) 键值对列表。数字商品的数量通常为 1。
     * @param couponCode 可选的折扣优惠券码（例如 "SAVE10" 表示 9 折）。
     * @return 包含已创建[Order]的 [Resource.Success]（状态 = PENDING），或 [Resource.Error]。
     */
    suspend fun createOrder(
        userId: String,
        items: List<Pair<String, Int>>,
        couponCode: String = ""
    ): Resource<Order>

    /**
     * 获取指定用户的所有订单。
     * 该列表包含所有状态的订单（PENDING、PAID、CANCELLED、REFUNDED）。
     * @param userId 要查询其订单的用户。
     */
    suspend fun getOrders(userId: String): Resource<List<Order>>

    /**
     * 获取单个订单的完整详情，包括其订单明细行。
     * @param orderId 要查找的订单 ID。
     */
    suspend fun getOrderDetail(orderId: String): Resource<Order>

    /**
     * 取消一个待支付（未付款）的订单。
     * @param orderId 要取消的订单。
     * @return 包含已取消[Order]的 [Resource.Success]（状态 = CANCELLED），或 [Resource.Error]。
     */
    suspend fun cancelOrder(orderId: String): Resource<Order>
}
