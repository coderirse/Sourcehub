package com.example.sourcehub.data.remote.dto

/**
 * 支付领域的传输对象。
 */

/**
 * 发起支付的请求体。
 *
 * @property orderId 要支付的订单。
 * @property amount 要收取的金额。
 * @property method 支付渠道名称（例如 "WECHAT", "ALIPAY"）。
 */
data class CreatePaymentRequest(
    val orderId: String,
    val amount: Double,
    val method: String
)

/**
 * 支付网关的响应。
 *
 * @property transactionId 网关交易参考号；失败时为空。
 * @property orderId 此支付对应的订单。
 * @property status "SUCCESS"、"FAILED" 或 "REFUNDED"。
 * @property amount 已处理的金额。
 * @property timestamp 交易时间戳（毫秒）。
 * @property errorCode 机器可读的错误码；成功时为空。
 * @property errorMessage 人类可读的错误描述；成功时为空。
 */
data class PaymentResponse(
    val transactionId: String,
    val orderId: String,
    val status: String,
    val amount: Double,
    val timestamp: Long,
    val errorCode: String = "",
    val errorMessage: String = ""
)
