package com.example.sourcehub.data.remote.api

import com.example.sourcehub.data.remote.dto.*

/**
 * 支付操作的 API 契约。
 *
 * 实现: [MockPaymentApi]（90% 成功率模拟）和
 * [RetrofitPaymentApi]（通过 Retrofit 调用 Ktor 后端；
 * 验证和退款服务端尚未实现）。
 */
interface PaymentApi {
    /** 为订单发起支付。 */
    suspend fun createPayment(request: CreatePaymentRequest): ApiResponse<PaymentResponse>

    /** 通过交易 ID 验证之前发起的支付。 */
    suspend fun verifyPayment(transactionId: String): ApiResponse<PaymentResponse>

    /** 为已支付订单请求退款。 */
    suspend fun refundPayment(orderId: String): ApiResponse<PaymentResponse>
}
