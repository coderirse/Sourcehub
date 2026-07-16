package com.example.sourcehub.data.repository

import com.example.sourcehub.data.remote.api.OrderApi
import com.example.sourcehub.data.remote.api.PaymentApi
import com.example.sourcehub.data.remote.dto.CreatePaymentRequest
import com.example.sourcehub.domain.model.PaymentMethod
import com.example.sourcehub.domain.model.PaymentResult
import com.example.sourcehub.domain.repository.PaymentRepository

/**
 * [PaymentRepository] 的实现，委托给 [PaymentApi]。
 *
 * ## 支付流程
 * 1. [processPayment] 将订单 ID、金额和支付方式发送到后端。
 * 2. 如果 API 返回状态 "SUCCESS"，则支付已确认，并返回
 *    带有交易 ID 的 [PaymentResult.Success]。
 * 3. 如果 API 返回任何其他状态（或错误代码），则返回
 *    带有错误详情的 [PaymentResult.Failure]。
 * 4. 网络/异常错误也被映射为 [PaymentResult.Failure]，
 *    代码为 "NETWORK_ERROR"。
 *
 * 模拟实现（[MockPaymentApi]）模拟约 90% 的成功率，
 * 并带有 1.5 秒的延迟来模拟真实支付处理。
 *
 * ## API 切换
 * [swapApi] 允许在模拟和 网络层 实现之间热切换。
 */
class PaymentRepositoryImpl(
    private var paymentApi: PaymentApi,
    private val orderApi: OrderApi
) : PaymentRepository {
    /** 在运行时替换底层 API（模拟 <-> 网络层）。 */
    fun swapApi(api: PaymentApi) { paymentApi = api }

    override suspend fun processPayment(orderId: String, amount: Double, method: PaymentMethod): PaymentResult {
        return try {
            val resp = paymentApi.createPayment(CreatePaymentRequest(orderId, amount, method.name))
            if (resp.code == 200 && resp.data != null) {
                // 检查支付网关响应中的状态字段。
                if (resp.data.status == "SUCCESS")
                    PaymentResult.Success(resp.data.transactionId, orderId, amount, resp.data.timestamp)
                else
                    PaymentResult.Failure(resp.data.errorCode, resp.data.errorMessage)
            } else PaymentResult.Failure("ERROR", resp.message)
        } catch (e: Exception) {
            // 网络级别的失败以支付失败的形式暴露，以便界面
            // 可以显示重试按钮而不是崩溃。
            PaymentResult.Failure("NETWORK_ERROR", e.message ?: "支付服务不可用")
        }
    }

    override suspend fun verifyPayment(transactionId: String): PaymentResult {
        return try {
            val resp = paymentApi.verifyPayment(transactionId)
            if (resp.code == 200 && resp.data != null)
                PaymentResult.Success(transactionId, resp.data.orderId, resp.data.amount)
            else PaymentResult.Failure("VERIFY_FAILED", resp.message)
        } catch (e: Exception) { PaymentResult.Failure("ERROR", e.message ?: "验证失败") }
    }

    override suspend fun refundPayment(orderId: String): PaymentResult {
        return try {
            val resp = paymentApi.refundPayment(orderId)
            if (resp.code == 200 && resp.data != null)
                PaymentResult.Success(resp.data.transactionId, orderId, resp.data.amount)
            else PaymentResult.Failure("REFUND_FAILED", resp.message)
        } catch (e: Exception) { PaymentResult.Failure("ERROR", e.message ?: "退款失败") }
    }
}
