package com.example.sourcehub.data.remote.retrofit

import com.example.sourcehub.data.remote.api.*
import com.example.sourcehub.data.remote.dto.*

/**
 * 桥接类，将 Retrofit 生成的服务适配到领域层
 * [Api] 接口。
 *
 * ## 为什么需要这些类
 *
 * 领域层在 `com.example.sourcehub.data.remote.api` 中定义了 API 契约
 * （例如 [AuthApi], [ProductApi]）。模拟实现和 Retrofit 实现
 * 都遵循这些接口，从而可以通过仓库的 `swapApi()` 方法在运行时切换。
 *
 * 每个桥接类：
 * 1. 接收一个 Retrofit 服务接口作为构造函数参数。
 * 2. 实现对应的领域 [Api] 接口。
 * 3. 将每个方法委托给 Retrofit 服务，执行必要的
 *    参数转换（例如将刷新令牌包装在 map 中，
 *    将 `limit`/`sort` 参数转换为查询参数）。
 *
 * ## 未实现的端点
 *
 * 一些领域 [Api] 方法还没有对应的 Retrofit 端点
 * （Ktor 后端尚未实现）。这些方法返回带有适当消息的
 * 存根 [ApiResponse]：
 * - [RetrofitAuthApi.forgotPassword] — "服务器端尚未实现"
 * - [RetrofitProductApi.getBanners], [getCategories] — 空列表
 * - [RetrofitOrderApi.cancelOrder] — "服务器端尚未实现"
 * - [RetrofitPaymentApi.verifyPayment], [refundPayment] — "尚未实现"
 * - [RetrofitDownloadApi.reportDownloadProgress] — 空 Unit 响应
 */

/**
 * 将 [RetrofitAuthService] 桥接到 [AuthApi]。
 *
 * 刷新令牌调用将字符串令牌包装在 map 中，
 * 因为 Retrofit 端点期望的 JSON 体为 `{"refreshToken":"..."}`。
 */
class RetrofitAuthApi(private val service: RetrofitAuthService) : AuthApi {
    override suspend fun login(request: LoginRequest) = service.login(request)
    override suspend fun register(request: RegisterRequest) = service.register(request)
    override suspend fun refreshToken(refreshToken: String) = service.refreshToken(mapOf("refreshToken" to refreshToken))
    override suspend fun getProfile() = service.getProfile()
    override suspend fun updateProfile(request: UpdateProfileRequest) = service.updateProfile(request)
    override suspend fun forgotPassword(email: String): ApiResponse<Unit> = ApiResponse(message = "Not implemented on server")
}

/**
 * 将 [RetrofitProductService] 桥接到 [ProductApi]。
 *
 * 横幅和分类在 Ktor 后端尚未实现，
 * 因此返回空列表。其他方法直接映射到 Retrofit
 * 调用，并进行适当的查询参数转换。
 */
class RetrofitProductApi(private val service: RetrofitProductService) : ProductApi {
    override suspend fun getBanners(): ApiResponse<List<BannerResponse>> = ApiResponse(data = emptyList())
    override suspend fun getCategories(): ApiResponse<List<CategoryResponse>> = ApiResponse(data = emptyList())
    override suspend fun getRecommendedProducts(limit: Int) = service.getRecommended()
    override suspend fun getNewArrivals(limit: Int) = service.getProducts(size = limit, sort = "newest")
    override suspend fun getProductsByCategory(categoryId: String) = service.getProducts(category = categoryId)
    override suspend fun getProductDetail(productId: String) = service.getDetail(productId)
    override suspend fun searchProducts(query: String) = service.search(query)
}

/**
 * 将 [RetrofitOrderService] 桥接到 [OrderApi]。
 *
 * [getOrders] Retrofit 端点不接受 userId 参数
 * （后端从 JWT 令牌中识别用户）。[cancelOrder]
 * 端点在 Ktor 后端尚未实现。
 */
class RetrofitOrderApi(private val service: RetrofitOrderService) : OrderApi {
    override suspend fun createOrder(request: CreateOrderRequest) = service.createOrder(request)
    override suspend fun getOrders(userId: String) = service.getOrders() // userId 在 JWT 中
    override suspend fun getOrderDetail(orderId: String) = service.getOrderDetail(orderId)
    override suspend fun cancelOrder(orderId: String): ApiResponse<OrderResponse> = ApiResponse(message = "Not implemented on server")
}

/**
 * 将 [RetrofitPaymentService] 桥接到 [PaymentApi]。
 *
 * 仅 [createPayment] 映射到真实的 Retrofit 端点 (`POST /api/payment/pay`)。
 * 验证和退款在 Ktor 后端尚未实现。
 */
class RetrofitPaymentApi(private val service: RetrofitPaymentService) : PaymentApi {
    override suspend fun createPayment(request: CreatePaymentRequest) = service.pay(request)
    override suspend fun verifyPayment(transactionId: String): ApiResponse<PaymentResponse> = ApiResponse(message = "Not implemented")
    override suspend fun refundPayment(orderId: String): ApiResponse<PaymentResponse> = ApiResponse(message = "Not implemented")
}

/**
 * 将 [RetrofitDownloadService] 桥接到 [DownloadApi]。
 *
 * [reportDownloadProgress] 在 Ktor 后端尚未实现
 * （进度由 [DownloadWorker] 在本地跟踪）。
 */
class RetrofitDownloadApi(private val service: RetrofitDownloadService) : DownloadApi {
    override suspend fun getDownloadUrl(productId: String) = service.getDownloadUrl(productId)
    override suspend fun reportDownloadProgress(request: DownloadProgressRequest): ApiResponse<Unit> = ApiResponse(data = Unit)
}
