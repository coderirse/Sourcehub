/**
 * SourceHub 应用的手动依赖注入容器。
 *
 * 此包提供手动编写的 DI 方案（无 Hilt/Koin/Dagger），功能如下：
 * - 创建并持有单例服务：数据库助手、偏好设置、令牌管理器。
 * - 通过 [toggleRemoteApi] 支持运行时 Mock ↔ 远程 API 切换。
 * - 暴露仓库实例，供表示层通过 ViewModel 消费。
 *
 * 该设计使 DI 层保持框架无关且易于调试——每个依赖项均显式连接，
 * 因此不会有代码生成或注解处理的意外问题。
 */
package com.example.sourcehub.di

import android.content.Context
import com.example.sourcehub.data.local.db.SourcehubDbHelper
import com.example.sourcehub.data.local.mock.MockDataProvider
import com.example.sourcehub.data.local.prefs.PreferencesManager
import com.example.sourcehub.data.remote.api.*
import com.example.sourcehub.data.remote.mock.*
import com.example.sourcehub.data.remote.retrofit.*
import com.example.sourcehub.data.repository.*
import com.example.sourcehub.domain.repository.*
import com.example.sourcehub.security.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 持有所有应用级依赖项。
 *
 * 每个需要在整个进程生命周期中存活的组件（数据库、偏好设置、
 * 令牌管理器、Mock 数据）都在此处创建。仓库由这些构建块组合而成，
 * 并通过全局 [SourcehubApplication.instance] 单例注入到 ViewModel 中。
 *
 * @param context 任意 Android [Context]；构造函数会立即将其提升为
 *                `applicationContext` 以避免 Activity 泄漏。
 */
class AppContainer(context: Context) {

    /** 应用上下文——可安全持有整个进程生命周期。 */
    private val applicationContext = context.applicationContext

    // ── 基础设施 ────────────────────────────────────────────────────────────────────

    /** 基于 [android.database.sqlite.SQLiteOpenHelper] 的 SQLite 数据库助手。 */
    val dbHelper = SourcehubDbHelper(applicationContext)

    /** 用于非敏感键值存储的 SharedPreferences 包装器。 */
    val preferencesManager = PreferencesManager(applicationContext)

    /** 加密的 JWT令牌 存储（基于 EncryptedSharedPreferences）。 */
    val tokenManager = TokenManager(applicationContext)

    // ── API 模式切换 ─────────────────────────────────────────────────────────────────

    /**
     * 当为 `true` 时，仓库将调用路由到真实的 Retrofit 支持的 API 层。
     * 当为 `false`（默认）时，所有调用访问内存中的 Mock 实现。
     * [StateFlow] 允许 UI 响应式地观察模式变化。
     */
    private val _useRemoteApi = MutableStateFlow(false)
    val useRemoteApi: StateFlow<Boolean> = _useRemoteApi.asStateFlow()

    // ── Mock 数据 ───────────────────────────────────────────────────────────────────

    /** 所有 Mock API 实现共享的始终可用的内存数据集。 */
    val mockDataProvider = MockDataProvider()

    // ── 远程 API（懒加载）───────────────────────────────────────────────────────────

    /**
     * Retrofit 支持的 API 服务集合。
     *
     * 懒初始化，以便 OkHttp 客户端和 Retrofit 实例仅在用户
     * 首次切换到远程模式时才创建——避免纯 Mock 会话中不必要的
     * 网络栈开销。
     */
    private val remoteApi: RetrofitApiSet by lazy { RetrofitApiSet() }

    // ── API 实现——访问时根据 [_useRemoteApi] 选择 ──────────────────────────────────

    private val authApi: AuthApi get() = if (_useRemoteApi.value) remoteApi.auth else mockAuth
    private val productApi: ProductApi get() = if (_useRemoteApi.value) remoteApi.product else mockProduct
    private val orderApi: OrderApi get() = if (_useRemoteApi.value) remoteApi.order else mockOrder
    private val paymentApi: PaymentApi get() = if (_useRemoteApi.value) remoteApi.payment else mockPayment
    private val downloadApi: DownloadApi get() = if (_useRemoteApi.value) remoteApi.download else mockDownload

    // ── Mock API 实例（始终存活）────────────────────────────────────────────────────

    val mockAuth = MockAuthApi(mockDataProvider)
    val mockProduct = MockProductApi(mockDataProvider)
    val mockOrder = MockOrderApi(mockDataProvider)
    val mockPayment = MockPaymentApi(mockDataProvider)
    val mockDownload = MockDownloadApi(mockDataProvider)

    // ── 仓库 ──────────────────────────────────────────────────────────────────────

    val authRepository: AuthRepository = AuthRepositoryImpl(mockAuth, tokenManager, preferencesManager, dbHelper)
    val productRepository: ProductRepository = ProductRepositoryImpl(mockProduct)
    val orderRepository: OrderRepository = OrderRepositoryImpl(mockOrder)

    /**
     * 支付仓库存储为私有 var，因为 [PaymentRepositoryImpl] 暴露了需要
     * 可变引用的 [swapApi] 方法。公共 getter 将其转型为
     * [PaymentRepository] 接口，防止外部代码调用 [swapApi]。
     */
    private val _paymentRepository = PaymentRepositoryImpl(mockPayment, mockOrder)
    val paymentRepository: PaymentRepository get() = _paymentRepository

    val downloadRepository: DownloadRepository = DownloadRepositoryImpl(mockDownload, dbHelper)
    val cartRepository: CartRepository = CartRepositoryImpl(dbHelper)

    /**
     * 在所有仓库 API 后端之间切换 Mock 和远程模式。
     *
     * 切换后，每个封装了 API 实现的仓库通过其 [swapApi] 方法
     * 原子性地更新。持有仓库接口引用的调用者将在下次调用时
     * 看到新的后端——无需重启。
     *
     * @param enabled 为 `true` 时使用 Retrofit 支持的远程 API，为 `false` 时使用 Mock 数据。
     */
    fun toggleRemoteApi(enabled: Boolean) {
        _useRemoteApi.value = enabled
        // 用新的 API 实现重建仓库
        (authRepository as AuthRepositoryImpl).swapApi(if (enabled) remoteApi.auth else mockAuth)
        (productRepository as ProductRepositoryImpl).swapApi(if (enabled) remoteApi.product else mockProduct)
        (orderRepository as OrderRepositoryImpl).swapApi(if (enabled) remoteApi.order else mockOrder)
        _paymentRepository.swapApi(if (enabled) remoteApi.payment else mockPayment)
        (downloadRepository as DownloadRepositoryImpl).swapApi(if (enabled) remoteApi.download else mockDownload)
    }
}

/**
 * 薄包装器，用于创建和持有所有基于 Retrofit 的 API 实现。
 *
 * 每个属性委托给从后端 OpenAPI 规范生成的 [RetrofitService] 接口。
 * 单个 [retrofit] 实例在所有服务间共享，以便 OkHttp 连接池和拦截器统一适用。
 *
 * 此类存在的目的是使 OkHttp/Retrofit 栈仅创建一次（且懒加载），
 * 而非每个 API 服务分别创建。
 */
class RetrofitApiSet {
    private val retrofit = ApiClient.build()

    val auth: AuthApi = RetrofitAuthApi(retrofit.create(RetrofitAuthService::class.java))
    val product: ProductApi = RetrofitProductApi(retrofit.create(RetrofitProductService::class.java))
    val order: OrderApi = RetrofitOrderApi(retrofit.create(RetrofitOrderService::class.java))
    val payment: PaymentApi = RetrofitPaymentApi(retrofit.create(RetrofitPaymentService::class.java))
    val download: DownloadApi = RetrofitDownloadApi(retrofit.create(RetrofitDownloadService::class.java))
}
