package com.example.sourcehub.domain.repository

import com.example.sourcehub.domain.model.Order
import com.example.sourcehub.presentation.common.state.Resource

interface OrderRepository {
    suspend fun createOrder(
        userId: String,
        items: List<Pair<String, Int>>,
        couponCode: String = ""
    ): Resource<Order>

    suspend fun getOrders(userId: String): Resource<List<Order>>
    suspend fun getOrderDetail(orderId: String): Resource<Order>
    suspend fun cancelOrder(orderId: String): Resource<Order>
}
