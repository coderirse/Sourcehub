package com.example.sourcehub.data.repository

import com.example.sourcehub.data.remote.api.OrderApi
import com.example.sourcehub.data.remote.dto.CreateOrderRequest
import com.example.sourcehub.data.remote.dto.OrderItemRequest
import com.example.sourcehub.domain.model.*
import com.example.sourcehub.domain.repository.OrderRepository
import com.example.sourcehub.presentation.common.state.Resource

class OrderRepositoryImpl(private val orderApi: OrderApi) : OrderRepository {

    override suspend fun createOrder(userId: String, items: List<Pair<String, Int>>, couponCode: String): Resource<Order> {
        return try {
            val r = orderApi.createOrder(CreateOrderRequest(userId, items.map { OrderItemRequest(it.first, it.second) }, couponCode))
            if (r.code == 200 && r.data != null) Resource.Success(r.data.toDomain())
            else Resource.Error(r.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "创建失败") }
    }

    override suspend fun getOrders(userId: String): Resource<List<Order>> {
        return try {
            val r = orderApi.getOrders(userId)
            if (r.code == 200 && r.data != null) Resource.Success(r.data.map { it.toDomain() })
            else Resource.Error(r.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "加载失败") }
    }

    override suspend fun getOrderDetail(orderId: String): Resource<Order> {
        return try {
            val r = orderApi.getOrderDetail(orderId)
            if (r.code == 200 && r.data != null) Resource.Success(r.data.toDomain())
            else Resource.Error(r.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "加载失败") }
    }

    override suspend fun cancelOrder(orderId: String): Resource<Order> {
        return try {
            val r = orderApi.cancelOrder(orderId)
            if (r.code == 200 && r.data != null) Resource.Success(r.data.toDomain())
            else Resource.Error(r.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "取消失败") }
    }

    private fun com.example.sourcehub.data.remote.dto.OrderResponse.toDomain(): Order {
        val orderItems = parseItems(itemsJson, id)
        return Order(id, userId, orderItems, totalAmount, discountAmount, finalAmount,
            try { OrderStatus.valueOf(status) } catch (e: Exception) { OrderStatus.PENDING },
            try { PaymentMethod.valueOf(paymentMethod) } catch (e: Exception) { PaymentMethod.WECHAT },
            transactionId, couponCode, createdAt, paidAt)
    }

    private fun parseItems(json: String, orderId: String): List<OrderItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val trimmed = json.trim().removePrefix("[").removeSuffix("]")
            if (trimmed.isBlank()) return emptyList()
            trimmed.split("},").mapNotNull { part ->
                val fields = parseJsonObject(part.trim())
                OrderItem("", orderId, fields["productId"] ?: "", fields["productTitle"] ?: "",
                    fields["productCover"] ?: "", fields["unitPrice"]?.toDoubleOrNull() ?: 0.0,
                    fields["quantity"]?.toIntOrNull() ?: 1)
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun parseJsonObject(raw: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val clean = raw.removeSurrounding("{", "}")
        var i = 0
        val chars = clean.toCharArray()
        while (i < chars.size) {
            while (i < chars.size && chars[i] in " \n\r\t,") i++
            if (i >= chars.size) break
            val ks = clean.indexOf('"', i) + 1
            val ke = clean.indexOf('"', ks)
            val key = clean.substring(ks, ke)
            i = clean.indexOf(':', ke) + 1
            while (i < chars.size && chars[i] in " \n\r\t") i++
            val vs = i
            val ve = if (chars[vs] == '"') { val end = clean.indexOf('"', vs + 1); if (end == -1) chars.size else end + 1 }
            else { var end = vs; while (end < chars.size && chars[end] !in ",\n\r}") end++; end }
            map[key] = clean.substring(vs, ve).trim().removeSurrounding("\"")
            i = ve
        }
        return map
    }
}
