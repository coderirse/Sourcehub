package com.example.sourcehub.di

import android.content.Context
import com.example.sourcehub.data.local.mock.MockDataProvider
import com.example.sourcehub.data.local.persistence.JsonPersistenceManager
import com.example.sourcehub.data.local.prefs.PreferencesManager
import com.example.sourcehub.data.remote.mock.MockAuthApi
import com.example.sourcehub.data.remote.mock.MockDownloadApi
import com.example.sourcehub.data.remote.mock.MockOrderApi
import com.example.sourcehub.data.remote.mock.MockPaymentApi
import com.example.sourcehub.data.remote.mock.MockProductApi
import com.example.sourcehub.data.repository.AuthRepositoryImpl
import com.example.sourcehub.data.repository.CartRepositoryImpl
import com.example.sourcehub.data.repository.DownloadRepositoryImpl
import com.example.sourcehub.data.repository.OrderRepositoryImpl
import com.example.sourcehub.data.repository.PaymentRepositoryImpl
import com.example.sourcehub.data.repository.ProductRepositoryImpl
import com.example.sourcehub.domain.repository.AuthRepository
import com.example.sourcehub.domain.repository.CartRepository
import com.example.sourcehub.domain.repository.DownloadRepository
import com.example.sourcehub.domain.repository.OrderRepository
import com.example.sourcehub.domain.repository.PaymentRepository
import com.example.sourcehub.domain.repository.ProductRepository
import com.example.sourcehub.security.TokenManager

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    // Preferences
    val preferencesManager = PreferencesManager(applicationContext)

    // Security
    val tokenManager = TokenManager(applicationContext)

    // Persistence (JSON-file based, zero annotation processing)
    val persistenceManager = JsonPersistenceManager(applicationContext)

    // Mock Data
    val mockDataProvider = MockDataProvider()

    // Mock APIs
    val mockAuthApi = MockAuthApi(mockDataProvider)
    val mockProductApi = MockProductApi(mockDataProvider)
    val mockOrderApi = MockOrderApi(mockDataProvider)
    val mockPaymentApi = MockPaymentApi(mockDataProvider)
    val mockDownloadApi = MockDownloadApi(mockDataProvider)

    // Repositories
    val authRepository: AuthRepository = AuthRepositoryImpl(mockAuthApi, tokenManager, preferencesManager, persistenceManager)
    val productRepository: ProductRepository = ProductRepositoryImpl(mockProductApi)
    val orderRepository: OrderRepository = OrderRepositoryImpl(mockOrderApi)
    val paymentRepository: PaymentRepository = PaymentRepositoryImpl(mockPaymentApi, mockOrderApi)
    val downloadRepository: DownloadRepository = DownloadRepositoryImpl(mockDownloadApi, persistenceManager)
    val cartRepository: CartRepository = CartRepositoryImpl(persistenceManager)
}
