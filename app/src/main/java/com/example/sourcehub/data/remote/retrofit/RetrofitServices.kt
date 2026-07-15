package com.example.sourcehub.data.remote.retrofit

import com.example.sourcehub.data.remote.dto.*
import retrofit2.http.*

interface RetrofitAuthService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<RegisterResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body body: Map<String, String>): ApiResponse<TokenResponse>

    @GET("api/auth/profile")
    suspend fun getProfile(): ApiResponse<UserProfileResponse>

    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<UserProfileResponse>
}

interface RetrofitProductService {
    @GET("api/products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("category") category: String? = null,
        @Query("sort") sort: String? = null
    ): ApiResponse<List<ProductResponse>>

    @GET("api/products/recommended")
    suspend fun getRecommended(): ApiResponse<List<ProductResponse>>

    @GET("api/products/search")
    suspend fun search(@Query("q") query: String): ApiResponse<List<ProductResponse>>

    @GET("api/products/{id}")
    suspend fun getDetail(@Path("id") id: String): ApiResponse<ProductResponse>
}

interface RetrofitOrderService {
    @POST("api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): ApiResponse<OrderResponse>

    @GET("api/orders")
    suspend fun getOrders(): ApiResponse<List<OrderResponse>>

    @GET("api/orders/{id}")
    suspend fun getOrderDetail(@Path("id") id: String): ApiResponse<OrderResponse>
}

interface RetrofitPaymentService {
    @POST("api/payment/pay")
    suspend fun pay(@Body request: CreatePaymentRequest): ApiResponse<PaymentResponse>
}

interface RetrofitDownloadService {
    @POST("api/files/download-url/{productId}")
    suspend fun getDownloadUrl(@Path("productId") productId: String): ApiResponse<DownloadUrlResponse>
}
