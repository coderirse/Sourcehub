package com.example.sourcehub.data.repository

import com.example.sourcehub.data.remote.api.OrderApi
import com.example.sourcehub.data.remote.dto.CreateOrderRequest
import com.example.sourcehub.data.remote.dto.OrderItemRequest
import com.example.sourcehub.domain.model.*
import com.example.sourcehub.domain.repository.OrderRepository
import com.example.sourcehub.presentation.common.state.Resource

/**
 * [OrderRepository] 的实现，委托给 [OrderApi]。
 *
 * ## 订单项 JSON 解析
 * 后端将订单行项目存储为 JSON 字符串（[OrderResponse.itemsJson]）
 * 而非结构化数组。[parseItems] 使用手写 JSON 解析器
 * 提取字段键值对。这避免了为处理受控的简单格式
 * 而引入完整的 JSON 库（如 Gson/Kotlinx Serialization）。
 *
 * 解析器处理：`[{"key":"value",...}, ...]`，其中值可以是
 * 带引号的字符串或不带引号的数字。
 *
 * ## API 切换
 * [swapApi] 允许在模拟和 网络层 实现之间热切换。
 */
class OrderRepositoryImpl(private var orderApi: OrderApi) : OrderRepository {
    /** 在运行时替换底层 API（模拟 <-> 网络层）。 */
    fun swapApi(api: OrderApi) { orderApi = api }

    override suspend fun createOrder(userId: String, items: List<Pair<String, Int>>, couponCode: String): Resource<Order> {
        return try {
            // 将 (productId, quantity) 键值对转换为 DTO 商品项列表。
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

    /**
     * 将 [OrderResponse] DTO 映射为领域 [Order] 模型。
     *
     * [itemsJson] 字符串被手动解析为 [OrderItem] 对象。
     * 枚举字段（[OrderStatus]、[PaymentMethod]）按名称解析，
     * 无法识别的值安全回退到 PENDING / WECHAT）。
     */
    private fun com.example.sourcehub.data.remote.dto.OrderResponse.toDomain(): Order {
        val orderItems = parseItems(itemsJson, id)
        return Order(id, userId, orderItems, totalAmount, discountAmount, finalAmount,
            try { OrderStatus.valueOf(status) } catch (e: Exception) { OrderStatus.PENDING },
            try { PaymentMethod.valueOf(paymentMethod) } catch (e: Exception) { PaymentMethod.WECHAT },
            transactionId, couponCode, createdAt, paidAt)
    }

    /**
     * 将手写 JSON 数组字符串解析为 [OrderItem] 对象列表。
     *
     * 期望格式：`[{"productId":"...","productTitle":"...","productCover":"...","unitPrice":9.9,"quantity":1}, ...]`
     *
     * 通过按 `},` 分割提取每个 JSON 对象，然后每个单独的对象
     * 由 [parseJsonObject] 解析。空字符串或空白字符串返回空列表。
     */
    private fun parseItems(json: String, orderId: String): List<OrderItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            // 去除外层方括号，并按 "}," 分割，以分离数组中的对象。
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

    /**
     * 用于解析单个 JSON 对象字符串（如 `{"key":"value","num":123}`）的最简手写解析器。
     *
     * 遍历字符以提取带引号的键及其值。
     * 值可以是带引号的字符串或不带引号的令牌（数字、布尔值）。
     * 这避免了为处理受约束、可预测的格式而依赖 Gson/Kotlinx Serialization。
     *
     * @param raw 原始 JSON 对象字符串，可选地被 `{` 和 `}` 包围。
     * @return 键 -> 值字符串的映射（值已去引号）。
     */
    private fun parseJsonObject(raw: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val clean = raw.removeSurrounding("{", "}")
        var i = 0
        val chars = clean.toCharArray()
        while (i < chars.size) {
            // 跳过键值对之间的空白字符和逗号。
            while (i < chars.size && chars[i] in " \n\r\t,") i++
            if (i >= chars.size) break
            // 提取带引号的键：跳过左引号，读取到右引号为止。
            val ks = clean.indexOf('"', i) + 1
            val ke = clean.indexOf('"', ks)
            val key = clean.substring(ks, ke)
            // 移动到冒号分隔符之后。
            i = clean.indexOf(':', ke) + 1
            while (i < chars.size && chars[i] in " \n\r\t") i++
            // 提取值。如果以引号开头，则为字符串；
            // 否则收集字符直到遇到逗号、换行或右花括号。
            val vs = i
            val ve = if (chars[vs] == '"') {
                val end = clean.indexOf('"', vs + 1); if (end == -1) chars.size else end + 1
            } else {
                var end = vs; while (end < chars.size && chars[end] !in ",\n\r}") end++; end
            }
            // 去除字符串值的引号并裁剪空白字符。
            map[key] = clean.substring(vs, ve).trim().removeSurrounding("\"")
            i = ve
        }
        return map
    }
}
