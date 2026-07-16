package com.example.sourcehub.domain.repository

import com.example.sourcehub.domain.model.PaymentMethod
import com.example.sourcehub.domain.model.PaymentResult

/**
 * 支付操作的契约接口。
 *
 * 与其他仓库不同，支付返回 [PaymentResult]（一个密封类）
 * 而非 [Resource]，因为支付结果有三种可能的
 * 结局：成功、失败或用户取消。
 */
interface PaymentRepository {

    /**
     * 为指定订单发起支付。
     *
     * 实现类会模拟或将请求委托给支付网关。
     * 模拟版本的支付成功率约为 90%，延迟为 1.5 秒。
     *
     * @param orderId 要支付的订单。
     * @param amount 要收取的金额。
     * @param method 支付渠道（微信、支付宝、信用卡）。
     * @return [PaymentResult.Success]、[PaymentResult.Failure] 或 [PaymentResult.Cancelled]。
     */
    suspend fun processPayment(orderId: String, amount: Double, method: PaymentMethod): PaymentResult

    /**
     * 根据交易 ID 验证之前发起的支付。
     * @param transactionId 来自 [processPayment] 的交易流水号。
     */
    suspend fun verifyPayment(transactionId: String): PaymentResult

    /**
     * 为已支付的订单申请退款。
     * @param orderId 要退款的订单。
     */
    suspend fun refundPayment(orderId: String): PaymentResult
}
