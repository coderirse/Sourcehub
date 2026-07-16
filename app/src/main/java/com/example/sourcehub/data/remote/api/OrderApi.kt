package com.example.sourcehub.data.remote.api

import com.example.sourcehub.data.remote.dto.*

/**
 * 订单管理的 API 契约。
 *
 * 实现: [MockOrderApi]（有状态的内存模拟）和
 * [RetrofitOrderApi]（通过 Retrofit 调用 Ktor 后端）。
 */
interface OrderApi {
    /** 从商品/数量对列表创建新订单。 */
    suspend fun createOrder(request: CreateOrderRequest): ApiResponse<OrderResponse>

    /** 列出指定用户的所有订单。 */
    suspend fun getOrders(userId: String): ApiResponse<List<OrderResponse>>

    /** 获取单个订单的完整详情，包括订单项。 */
    suspend fun getOrderDetail(orderId: String): ApiResponse<OrderResponse>

    /** 取消一个待支付（未支付）订单。 */
    suspend fun cancelOrder(orderId: String): ApiResponse<OrderResponse>
}
