package com.example.sourcehub.domain.repository

import com.example.sourcehub.domain.model.PaymentMethod
import com.example.sourcehub.domain.model.PaymentResult

interface PaymentRepository {
    suspend fun processPayment(orderId: String, amount: Double, method: PaymentMethod): PaymentResult
    suspend fun verifyPayment(transactionId: String): PaymentResult
    suspend fun refundPayment(orderId: String): PaymentResult
}
