package com.example.sourcehub.data.remote.api

import com.example.sourcehub.data.remote.dto.*

interface OrderApi {
    suspend fun createOrder(request: CreateOrderRequest): ApiResponse<OrderResponse>
    suspend fun getOrders(userId: String): ApiResponse<List<OrderResponse>>
    suspend fun getOrderDetail(orderId: String): ApiResponse<OrderResponse>
    suspend fun cancelOrder(orderId: String): ApiResponse<OrderResponse>
}
