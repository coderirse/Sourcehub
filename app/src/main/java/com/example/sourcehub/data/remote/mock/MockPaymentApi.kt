package com.example.sourcehub.data.remote.mock

import com.example.sourcehub.data.remote.api.PaymentApi
import com.example.sourcehub.data.remote.dto.*
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * [PaymentApi] 的内存模拟实现，用于开发和测试。
 *
 * ## 核心行为: 90% 成功率
 *
 * [createPayment] 模拟支付网关，具有约 90% 的成功率
 * (`Random.nextInt(10) > 1`)。成功时：
 * 1. 生成模拟交易 ID (`txn_...`)。
 * 2. 将支付记录在 [MockDataProvider.paidOrders] 中，
 *    以便 [MockOrderApi.syncPaidOrders] 可以将订单状态更新为 PAID。
 * 3. 返回状态为 "SUCCESS" 的 [PaymentResponse]。
 *
 * 失败时（10% 概率）返回 500 响应，状态为 "FAILED"
 * 并附带错误详情，模拟交易被拒绝。
 *
 * ## 固定延迟
 * 与其他模拟 API 不同，[createPayment] 使用 **固定** 1.5 秒延迟
 * （无随机性）以模拟支付网关调用的真实延迟。
 * 这给界面足够时间显示加载动画。
 *
 * ## 其他端点
 *
 * - **[verifyPayment]**: 无论交易 ID 如何，始终返回 SUCCESS。
 *   延迟: 300-600 毫秒。
 * - **[refundPayment]**: 始终返回 REFUNDED 并附带生成的退款
 *   交易 ID。延迟: 500-1000 毫秒。
 */
class MockPaymentApi(private val mockDataProvider: com.example.sourcehub.data.local.mock.MockDataProvider) : PaymentApi {

    override suspend fun createPayment(request: CreatePaymentRequest): ApiResponse<PaymentResponse> {
        // 固定 1.5 秒延迟以模拟真实的支付处理时间。
        delay(1500)

        // 90% 成功率: Random.nextInt(10) 返回 0-9，值 > 1 意味着
        // 10 中 8 = 80%，但从 1 开始意味着 10 中 9 = 90%。
        val success = Random.nextInt(10) > 1
        return if (success) {
            val txnId = "txn_${SecurityUtils.generateUuid().take(12)}"
            val ts = System.currentTimeMillis()
            // 写入共享状态，使 MockOrderApi 可以将订单同步为 PAID。
            mockDataProvider.paidOrders[request.orderId] = Pair(txnId, ts)
            ApiResponse(
                data = PaymentResponse(
                    transactionId = txnId,
                    orderId = request.orderId,
                    status = "SUCCESS",
                    amount = request.amount,
                    timestamp = ts
                )
            )
        } else {
            // 模拟交易被拒绝。
            ApiResponse(
                code = 500,
                message = "支付失败",
                data = PaymentResponse(
                    transactionId = "",
                    orderId = request.orderId,
                    status = "FAILED",
                    amount = request.amount,
                    timestamp = System.currentTimeMillis(),
                    errorCode = "PAYMENT_FAILED",
                    errorMessage = "支付处理失败，请重试"
                )
            )
        }
    }

    override suspend fun verifyPayment(transactionId: String): ApiResponse<PaymentResponse> {
        delay(Random.nextLong(300, 600))
        // 始终返回成功 — 模拟中不做真正的验证。
        return ApiResponse(
            data = PaymentResponse(
                transactionId = transactionId,
                orderId = "",
                status = "SUCCESS",
                amount = 0.0,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun refundPayment(orderId: String): ApiResponse<PaymentResponse> {
        delay(Random.nextLong(500, 1000))
        // 始终成功，返回生成的退款交易 ID。
        return ApiResponse(
            data = PaymentResponse(
                transactionId = "refund_${SecurityUtils.generateUuid().take(12)}",
                orderId = orderId,
                status = "REFUNDED",
                amount = 0.0,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
