package com.example.sourcehub.data.remote.mock

import com.example.sourcehub.data.local.mock.MockDataProvider
import com.example.sourcehub.data.remote.api.OrderApi
import com.example.sourcehub.data.remote.dto.*
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.delay
import kotlin.random.Random

class MockOrderApi(private val mockData: MockDataProvider) : OrderApi {

    private val orders = mutableListOf<OrderResponse>()

    override suspend fun createOrder(request: CreateOrderRequest): ApiResponse<OrderResponse> {
        delay(Random.nextLong(500, 1000))
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
        val totalAmount = items.sumOf { (it["unitPrice"] as Double) * (it["quantity"] as Int) }
        var discountAmount = 0.0
        if (request.couponCode == "SAVE10") discountAmount = totalAmount * 0.1

        // Build items JSON string manually
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
            val cancelled = orders[idx].copy(status = "CANCELLED")
            orders[idx] = cancelled
            ApiResponse(data = cancelled)
        } else ApiResponse(code = 404, message = "订单不存在")
    }

    // Sync paid status from MockPaymentApi via MockDataProvider
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
