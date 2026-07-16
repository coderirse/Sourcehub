package com.example.sourcehub.data.remote.retrofit

import com.example.sourcehub.data.remote.dto.*
import retrofit2.http.*

/**
 * Sourcehub 后端 API 的 Retrofit 服务接口。
 *
 * 每个接口将一个逻辑领域（认证、商品、订单、支付、
 * 下载）映射到 Ktor 后端服务器暴露的 HTTP 端点。
 * 这些接口由 [ApiClient.build] 使用，
 * 通过 Retrofit 的动态代理在运行时生成类型安全的 HTTP 调用实现。
 *
 * ## 命名约定
 * 每个接口以 `Retrofit` 为前缀，以区别于
 * [com.example.sourcehub.data.remote.api] 领域层接口，
 * 模拟和 Retrofit 实现都遵循这些领域层接口。
 */

/**
 * 认证端点。
 *
 * 基础路径: `/api/auth/`
 */
interface RetrofitAuthService {
    /** POST /api/auth/login — 使用邮箱和密码进行认证。 */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    /** POST /api/auth/register — 创建新账号。 */
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<RegisterResponse>

    /** POST /api/auth/refresh — 用刷新令牌换取新的访问令牌。 */
    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body body: Map<String, String>): ApiResponse<TokenResponse>

    /** GET /api/auth/profile — 获取当前用户的个人资料。 */
    @GET("api/auth/profile")
    suspend fun getProfile(): ApiResponse<UserProfileResponse>

    /** PUT /api/auth/profile — 更新当前用户的个人资料。 */
    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<UserProfileResponse>

    /** POST /api/auth/forgot-password — 发送密码重置邮件。 */
    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: Map<String, String>): ApiResponse<Unit>
}

/** 横幅和分类端点。 */
interface RetrofitBannerService {
    @GET("api/banners") suspend fun getBanners(): ApiResponse<List<BannerResponse>>
    @GET("api/categories") suspend fun getCategories(): ApiResponse<List<CategoryResponse>>
}

/**
 * 商品目录端点。
 *
 * 基础路径: `/api/products/`
 */
interface RetrofitProductService {
    /**
     * GET /api/products — 分页商品列表，支持可选的分类
     * 和排序筛选。
     */
    @GET("api/products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("category") category: String? = null,
        @Query("sort") sort: String? = null
    ): ApiResponse<List<ProductResponse>>

    /** GET /api/products/recommended — 推荐商品（按销量排序）。 */
    @GET("api/products/recommended")
    suspend fun getRecommended(): ApiResponse<List<ProductResponse>>

    /** GET /api/products/search?q= — 全文搜索商品。 */
    @GET("api/products/search")
    suspend fun search(@Query("q") query: String): ApiResponse<List<ProductResponse>>

    /** GET /api/products/{id} — 单个商品详情。 */
    @GET("api/products/{id}")
    suspend fun getDetail(@Path("id") id: String): ApiResponse<ProductResponse>
}

/**
 * 订单管理端点。
 *
 * 基础路径: `/api/orders/`
 */
interface RetrofitOrderService {
    /** POST /api/orders — 从购物车商品创建新订单。 */
    @POST("api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): ApiResponse<OrderResponse>

    /** GET /api/orders — 列出已认证用户的订单。 */
    @GET("api/orders")
    suspend fun getOrders(): ApiResponse<List<OrderResponse>>

    /** GET /api/orders/{id} — 单个订单详情及订单项。 */
    @GET("api/orders/{id}")
    suspend fun getOrderDetail(@Path("id") id: String): ApiResponse<OrderResponse>

    /** POST /api/orders/{id}/cancel — 取消待支付订单。 */
    @POST("api/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: String): ApiResponse<OrderResponse>
}

/**
 * 支付端点。
 *
 * 基础路径: `/api/payment/`
 */
interface RetrofitPaymentService {
    /** POST /api/payment/pay — 为订单发起支付。 */
    @POST("api/payment/pay")
    suspend fun pay(@Body request: CreatePaymentRequest): ApiResponse<PaymentResponse>

    /** POST /api/payment/verify — 通过交易ID验证支付。 */
    @POST("api/payment/verify")
    suspend fun verify(@Body body: Map<String, String>): ApiResponse<PaymentResponse>

    /** POST /api/payment/refund — 为已支付订单退款。 */
    @POST("api/payment/refund")
    suspend fun refund(@Body body: Map<String, String>): ApiResponse<PaymentResponse>
}

/**
 * 文件下载端点。
 *
 * 基础路径: `/api/files/`
 */
interface RetrofitDownloadService {
    /** POST /api/files/download-url/{productId} — 获取预签名下载 URL。 */
    @POST("api/files/download-url/{productId}")
    suspend fun getDownloadUrl(@Path("productId") productId: String): ApiResponse<DownloadUrlResponse>
}
