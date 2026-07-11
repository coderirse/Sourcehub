package com.example.sourcehub.data.remote.mock

import com.example.sourcehub.data.remote.api.PaymentApi
import com.example.sourcehub.data.remote.dto.*
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.delay
import kotlin.random.Random

class MockPaymentApi(private val mockDataProvider: com.example.sourcehub.data.local.mock.MockDataProvider) : PaymentApi {

    override suspend fun createPayment(request: CreatePaymentRequest): ApiResponse<PaymentResponse> {
        delay(1500) // Simulate payment processing
        val success = Random.nextInt(10) > 1 // 90% success rate
        return if (success) {
            ApiResponse(
                data = PaymentResponse(
                    transactionId = "txn_${SecurityUtils.generateUuid().take(12)}",
                    orderId = request.orderId,
                    status = "SUCCESS",
                    amount = request.amount,
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
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
