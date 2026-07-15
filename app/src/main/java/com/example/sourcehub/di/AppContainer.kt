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

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    // Database
    val dbHelper = SourcehubDbHelper(applicationContext)

    // Preferences
    val preferencesManager = PreferencesManager(applicationContext)

    // Security
    val tokenManager = TokenManager(applicationContext)

    // API mode (mock by default, toggle to remote in Settings)
    private val _useRemoteApi = MutableStateFlow(false)
    val useRemoteApi: StateFlow<Boolean> = _useRemoteApi.asStateFlow()

    // Mock Data (always available)
    val mockDataProvider = MockDataProvider()

    // Retrofit services (lazy, only initialized when remote mode is enabled)
    private val remoteApi: RetrofitApiSet by lazy { RetrofitApiSet() }

    // API implementations — read useRemoteApi to decide
    private val authApi: AuthApi get() = if (_useRemoteApi.value) remoteApi.auth else mockAuth
    private val productApi: ProductApi get() = if (_useRemoteApi.value) remoteApi.product else mockProduct
    private val orderApi: OrderApi get() = if (_useRemoteApi.value) remoteApi.order else mockOrder
    private val paymentApi: PaymentApi get() = if (_useRemoteApi.value) remoteApi.payment else mockPayment
    private val downloadApi: DownloadApi get() = if (_useRemoteApi.value) remoteApi.download else mockDownload

    // Mock APIs
    val mockAuth = MockAuthApi(mockDataProvider)
    val mockProduct = MockProductApi(mockDataProvider)
    val mockOrder = MockOrderApi(mockDataProvider)
    val mockPayment = MockPaymentApi(mockDataProvider)
    val mockDownload = MockDownloadApi(mockDataProvider)

    // Repositories
    val authRepository: AuthRepository = AuthRepositoryImpl(mockAuth, tokenManager, preferencesManager, dbHelper)
    val productRepository: ProductRepository = ProductRepositoryImpl(mockProduct)
    val orderRepository: OrderRepository = OrderRepositoryImpl(mockOrder)
    private val _paymentRepository = PaymentRepositoryImpl(mockPayment, mockOrder)
    val paymentRepository: PaymentRepository get() = _paymentRepository
    val downloadRepository: DownloadRepository = DownloadRepositoryImpl(mockDownload, dbHelper)
    val cartRepository: CartRepository = CartRepositoryImpl(dbHelper)

    fun toggleRemoteApi(enabled: Boolean) {
        _useRemoteApi.value = enabled
        // Recreate repositories with new API implementations
        (authRepository as AuthRepositoryImpl).swapApi(if (enabled) remoteApi.auth else mockAuth)
        (productRepository as ProductRepositoryImpl).swapApi(if (enabled) remoteApi.product else mockProduct)
        (orderRepository as OrderRepositoryImpl).swapApi(if (enabled) remoteApi.order else mockOrder)
        _paymentRepository.swapApi(if (enabled) remoteApi.payment else mockPayment)
        (downloadRepository as DownloadRepositoryImpl).swapApi(if (enabled) remoteApi.download else mockDownload)
    }
}

/** Lazily-initialized Retrofit service set for remote API mode. */
class RetrofitApiSet {
    private val retrofit = ApiClient.build()

    val auth: AuthApi = RetrofitAuthApi(retrofit.create(RetrofitAuthService::class.java))
    val product: ProductApi = RetrofitProductApi(retrofit.create(RetrofitProductService::class.java))
    val order: OrderApi = RetrofitOrderApi(retrofit.create(RetrofitOrderService::class.java))
    val payment: PaymentApi = RetrofitPaymentApi(retrofit.create(RetrofitPaymentService::class.java))
    val download: DownloadApi = RetrofitDownloadApi(retrofit.create(RetrofitDownloadService::class.java))
}
