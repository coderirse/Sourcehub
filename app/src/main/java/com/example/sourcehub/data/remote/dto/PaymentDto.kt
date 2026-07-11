package com.example.sourcehub.data.remote.dto

data class CreatePaymentRequest(
    val orderId: String,
    val amount: Double,
    val method: String
)

data class PaymentResponse(
    val transactionId: String,
    val orderId: String,
    val status: String,
    val amount: Double,
    val timestamp: Long,
    val errorCode: String = "",
    val errorMessage: String = ""
)
