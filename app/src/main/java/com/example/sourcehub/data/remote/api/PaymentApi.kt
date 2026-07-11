package com.example.sourcehub.data.remote.api

import com.example.sourcehub.data.remote.dto.*

interface PaymentApi {
    suspend fun createPayment(request: CreatePaymentRequest): ApiResponse<PaymentResponse>
    suspend fun verifyPayment(transactionId: String): ApiResponse<PaymentResponse>
    suspend fun refundPayment(orderId: String): ApiResponse<PaymentResponse>
}
