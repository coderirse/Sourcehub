package com.example.sourcehub.data.repository

import com.example.sourcehub.data.remote.api.OrderApi
import com.example.sourcehub.data.remote.api.PaymentApi
import com.example.sourcehub.data.remote.dto.CreatePaymentRequest
import com.example.sourcehub.domain.model.PaymentMethod
import com.example.sourcehub.domain.model.PaymentResult
import com.example.sourcehub.domain.repository.PaymentRepository

class PaymentRepositoryImpl(
    private var paymentApi: PaymentApi,
    private val orderApi: OrderApi
) : PaymentRepository {
    fun swapApi(api: PaymentApi) { paymentApi = api }

    override suspend fun processPayment(orderId: String, amount: Double, method: PaymentMethod): PaymentResult {
        return try {
            val resp = paymentApi.createPayment(CreatePaymentRequest(orderId, amount, method.name))
            if (resp.code == 200 && resp.data != null) {
                if (resp.data.status == "SUCCESS") PaymentResult.Success(resp.data.transactionId, orderId, amount, resp.data.timestamp)
                else PaymentResult.Failure(resp.data.errorCode, resp.data.errorMessage)
            } else PaymentResult.Failure("ERROR", resp.message)
        } catch (e: Exception) { PaymentResult.Failure("NETWORK_ERROR", e.message ?: "支付服务不可用") }
    }

    override suspend fun verifyPayment(transactionId: String): PaymentResult {
        return try {
            val resp = paymentApi.verifyPayment(transactionId)
            if (resp.code == 200 && resp.data != null) PaymentResult.Success(transactionId, resp.data.orderId, resp.data.amount)
            else PaymentResult.Failure("VERIFY_FAILED", resp.message)
        } catch (e: Exception) { PaymentResult.Failure("ERROR", e.message ?: "验证失败") }
    }

    override suspend fun refundPayment(orderId: String): PaymentResult {
        return try {
            val resp = paymentApi.refundPayment(orderId)
            if (resp.code == 200 && resp.data != null) PaymentResult.Success(resp.data.transactionId, orderId, resp.data.amount)
            else PaymentResult.Failure("REFUND_FAILED", resp.message)
        } catch (e: Exception) { PaymentResult.Failure("ERROR", e.message ?: "退款失败") }
    }
}
