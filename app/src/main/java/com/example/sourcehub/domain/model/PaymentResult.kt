package com.example.sourcehub.domain.model

/**
 * 表示支付操作结果的密封类层次结构。
 *
 * 支付流程不抛出异常，而是返回三种可能的结果之一。
 * 界面层对这些结果进行穷举匹配，以显示成功确认、
 * 错误对话框或返回订单页面。
 */
sealed class PaymentResult {

    /**
     * 支付已成功处理。
     *
     * @property transactionId 支付网关交易参考号。
     * @property orderId 已支付完成的订单。
     * @property amount 已收取的金额。
     * @property timestamp 支付成功的毫秒时间戳。
     */
    data class Success(
        val transactionId: String,
        val orderId: String,
        val amount: Double,
        val timestamp: Long = System.currentTimeMillis()
    ) : PaymentResult()

    /**
     * 支付失败 — 可能被网关拒绝或由于网络错误。
     *
     * @property errorCode 机器可读的错误代码（例如 "PAYMENT_FAILED"、"NETWORK_ERROR"）。
     * @property errorMessage 人类可读的中文错误描述。
     */
    data class Failure(
        val errorCode: String,
        val errorMessage: String
    ) : PaymentResult()

    /**
     * 用户在支付完成前明确取消了支付流程。
     */
    data object Cancelled : PaymentResult()
}
