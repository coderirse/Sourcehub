package com.example.sourcehub.data.remote.mock

import com.example.sourcehub.data.local.mock.MockDataProvider
import com.example.sourcehub.data.remote.api.OrderApi
import com.example.sourcehub.data.remote.dto.*
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * [OrderApi] 的内存模拟实现，用于开发和测试。
 *
 * ## 有状态性
 * 与其他模拟 API 不同，此类维护可变状态：
 * 一个累积已创建订单的 `private val orders` 列表。这使得：
 * - [getOrders] 和 [getOrderDetail] 可以返回之前创建的订单。
 * - [cancelOrder] 可以就地修改订单状态。
 * - [syncPaidOrders] 在 [MockPaymentApi] 记录成功支付后更新订单状态。
 *
 * ## 支付同步
 * [syncPaidOrders] 在每次读取操作前被调用。它检查
 * [MockDataProvider.paidOrders]（由 [MockPaymentApi] 填充）并
 * 将匹配的订单更新为 PAID 状态，包含交易 ID 和支付时间戳。
 *
 * ## 优惠券支持
 * 优惠券码 `"SAVE10"` 对订单总金额应用 10% 折扣。
 * 折扣在 [createOrder] 中客户端计算。
 *
 * ## 商品 JSON
 * 订单项序列化为手工构建的 JSON 字符串 ([itemsJson])，
 * 因为 [OrderResponse] 模型使用扁平的 JSON 字符串而非
 * 结构化数组。这与后端传输格式匹配。
 *
 * ## 延迟
 * - createOrder: 500-1000 毫秒
 * - getOrders: 300-600 毫秒
 * - getOrderDetail: 200-500 毫秒
 * - cancelOrder: 300-600 毫秒
 */
class MockOrderApi(private val mockData: MockDataProvider) : OrderApi {

    /** 会话期间累积的订单。应用重启后不持久化。 */
    private val orders = mutableListOf<OrderResponse>()

    override suspend fun createOrder(request: CreateOrderRequest): ApiResponse<OrderResponse> {
        delay(Random.nextLong(500, 1000))

        // 从模拟目录中解析每个订单项的对应商品详情。
        val items = request.items.mapNotNull { itemReq ->
            mockData.getProductById(itemReq.productId)?.let { product ->
                mapOf(
                    "productId" to product.id,
                    "productTitle" to product.title,
                    "productCover" to product.coverUrl,
                    "unitPrice" to product.price,
                    "quantity" to itemReq.quantity
                )
            }
        }

        // 计算总金额。优惠券码 "SAVE10" 享受 10% 折扣。
        val totalAmount = items.sumOf { (it["unitPrice"] as Double) * (it["quantity"] as Int) }
        var discountAmount = 0.0
        if (request.couponCode == "SAVE10") discountAmount = totalAmount * 0.1

        // 手动构建订单项 JSON 字符串 — 与后端传输格式一致。
        val itemsJson = buildString {
            append("[")
            items.forEachIndexed { index, item ->
                if (index > 0) append(",")
                append("""{"productId":"${item["productId"]}",""")
                append("""productTitle":"${item["productTitle"]}",""")
                append("""productCover":"${item["productCover"]}",""")
                append("""unitPrice":${item["unitPrice"]},""")
                append("""quantity":${item["quantity"]}}""")
            }
            append("]")
        }

        val order = OrderResponse(
            id = "order_${SecurityUtils.generateUuid().take(8)}",
            userId = request.userId,
            itemsJson = itemsJson,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            finalAmount = totalAmount - discountAmount,
            status = "PENDING",
            paymentMethod = "WECHAT",
            transactionId = "",
            couponCode = request.couponCode,
            createdAt = System.currentTimeMillis(),
            paidAt = 0L
        )
        orders.add(order)
        return ApiResponse(data = order)
    }

    override suspend fun getOrders(userId: String): ApiResponse<List<OrderResponse>> {
        delay(Random.nextLong(300, 600))
        // 返回前同步支付状态，让新支付的订单显示出来。
        syncPaidOrders()
        return ApiResponse(data = orders.filter { it.userId == userId })
    }

    override suspend fun getOrderDetail(orderId: String): ApiResponse<OrderResponse> {
        delay(Random.nextLong(200, 500))
        syncPaidOrders()
        val order = orders.find { it.id == orderId }
        return if (order != null) ApiResponse(data = order)
        else ApiResponse(code = 404, message = "订单不存在")
    }

    override suspend fun cancelOrder(orderId: String): ApiResponse<OrderResponse> {
        delay(Random.nextLong(300, 600))
        val idx = orders.indexOfFirst { it.id == orderId }
        return if (idx >= 0) {
            // 用已取消的副本就地替换该订单。
            val cancelled = orders[idx].copy(status = "CANCELLED")
            orders[idx] = cancelled
            ApiResponse(data = cancelled)
        } else ApiResponse(code = 404, message = "订单不存在")
    }

    /**
     * 通过 [MockDataProvider.paidOrders] 从 [MockPaymentApi] 同步已支付状态。
     *
     * 当 [MockPaymentApi.createPayment] 成功时，它将订单 ID、
     * 交易 ID 和时间戳写入 [MockDataProvider.paidOrders]。
     * 此方法读取该映射并更新本地 [orders] 列表，
     * 使后续 [getOrders] / [getOrderDetail] 调用能反映已支付状态。
     */
    private fun syncPaidOrders() {
        mockData.paidOrders.forEach { (orderId, paidInfo) ->
            val idx = orders.indexOfFirst { it.id == orderId }
            if (idx >= 0) {
                val order = orders[idx]
                orders[idx] = order.copy(
                    status = "PAID",
                    transactionId = paidInfo.first,
                    paidAt = paidInfo.second
                )
            }
        }
    }
}
