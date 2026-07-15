package com.example.sourcehub.data.remote.retrofit

import com.example.sourcehub.data.remote.api.*
import com.example.sourcehub.data.remote.dto.*

class RetrofitAuthApi(private val service: RetrofitAuthService) : AuthApi {
    override suspend fun login(request: LoginRequest) = service.login(request)
    override suspend fun register(request: RegisterRequest) = service.register(request)
    override suspend fun refreshToken(refreshToken: String) = service.refreshToken(mapOf("refreshToken" to refreshToken))
    override suspend fun getProfile() = service.getProfile()
    override suspend fun updateProfile(request: UpdateProfileRequest) = service.updateProfile(request)
    override suspend fun forgotPassword(email: String): ApiResponse<Unit> = ApiResponse(message = "Not implemented on server")
}

class RetrofitProductApi(private val service: RetrofitProductService) : ProductApi {
    override suspend fun getBanners(): ApiResponse<List<BannerResponse>> = ApiResponse(data = emptyList())
    override suspend fun getCategories(): ApiResponse<List<CategoryResponse>> = ApiResponse(data = emptyList())
    override suspend fun getRecommendedProducts(limit: Int) = service.getRecommended()
    override suspend fun getNewArrivals(limit: Int) = service.getProducts(size = limit, sort = "newest")
    override suspend fun getProductsByCategory(categoryId: String) = service.getProducts(category = categoryId)
    override suspend fun getProductDetail(productId: String) = service.getDetail(productId)
    override suspend fun searchProducts(query: String) = service.search(query)
}

class RetrofitOrderApi(private val service: RetrofitOrderService) : OrderApi {
    override suspend fun createOrder(request: CreateOrderRequest) = service.createOrder(request)
    override suspend fun getOrders(userId: String) = service.getOrders()
    override suspend fun getOrderDetail(orderId: String) = service.getOrderDetail(orderId)
    override suspend fun cancelOrder(orderId: String): ApiResponse<OrderResponse> = ApiResponse(message = "Not implemented on server")
}

class RetrofitPaymentApi(private val service: RetrofitPaymentService) : PaymentApi {
    override suspend fun createPayment(request: CreatePaymentRequest) = service.pay(request)
    override suspend fun verifyPayment(transactionId: String): ApiResponse<PaymentResponse> = ApiResponse(message = "Not implemented")
    override suspend fun refundPayment(orderId: String): ApiResponse<PaymentResponse> = ApiResponse(message = "Not implemented")
}

class RetrofitDownloadApi(private val service: RetrofitDownloadService) : DownloadApi {
    override suspend fun getDownloadUrl(productId: String) = service.getDownloadUrl(productId)
    override suspend fun reportDownloadProgress(request: DownloadProgressRequest): ApiResponse<Unit> = ApiResponse(data = Unit)
}
