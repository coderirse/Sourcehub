package com.example.sourcehub.domain.model

sealed class PaymentResult {
    data class Success(
        val transactionId: String,
        val orderId: String,
        val amount: Double,
        val timestamp: Long = System.currentTimeMillis()
    ) : PaymentResult()

    data class Failure(
        val errorCode: String,
        val errorMessage: String
    ) : PaymentResult()

    data object Cancelled : PaymentResult()
}
