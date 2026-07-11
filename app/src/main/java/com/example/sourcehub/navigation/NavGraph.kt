package com.example.sourcehub.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.presentation.auth.*
import com.example.sourcehub.presentation.cart.*
import com.example.sourcehub.presentation.checkout.*
import com.example.sourcehub.presentation.download.*
import com.example.sourcehub.presentation.home.*
import com.example.sourcehub.presentation.orders.detail.*
import com.example.sourcehub.presentation.orders.list.*
import com.example.sourcehub.presentation.payment.*
import com.example.sourcehub.presentation.product.detail.*
import com.example.sourcehub.presentation.product.list.*
import com.example.sourcehub.presentation.product.preview.*
import com.example.sourcehub.presentation.profile.*
import com.example.sourcehub.presentation.search.*
import com.example.sourcehub.presentation.settings.*
import com.example.sourcehub.security.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val appContainer = remember { SourcehubApplication.instance.appContainer }
    val isLoggedIn by appContainer.tokenManager.isLoggedIn.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route

    // React to login state changes
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Home
        composable(Screen.Home.route) {
            HomeScreen(
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onCategoryClick = { categoryId, categoryName ->
                    navController.navigate(Screen.ProductList.createRoute(categoryId, categoryName))
                },
                onBannerClick = { banner ->
                    when (banner.linkType.name) {
                        "PRODUCT" -> navController.navigate(Screen.ProductDetail.createRoute(banner.linkValue))
                        "CATEGORY" -> navController.navigate(Screen.ProductList.createRoute(banner.linkValue, banner.title))
                    }
                }
            )
        }

        // Search
        composable(Screen.Search.route) {
            SearchScreen(
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                }
            )
        }

        // Product
        composable(
            route = Screen.ProductList.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType; defaultValue = "" },
                navArgument("categoryName") { type = NavType.StringType; defaultValue = "全部商品" }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: "全部商品"
            ProductListScreen(
                categoryId = categoryId,
                categoryName = categoryName,
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() },
                onPreview = { navController.navigate(Screen.ProductPreview.createRoute(productId)) },
                onBuyNow = { orderId ->
                    navController.navigate(Screen.Payment.createRoute(orderId))
                },
                onAddToCartSuccess = {}
            )
        }

        composable(
            route = Screen.ProductPreview.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductPreviewScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Cart
        composable(Screen.Cart.route) {
            CartScreen(
                onCheckout = { navController.navigate(Screen.Checkout.route) },
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                }
            )
        }

        // Checkout
        composable(Screen.Checkout.route) {
            CheckoutScreen(
                onNavigateBack = { navController.popBackStack() },
                onPayment = { orderId ->
                    navController.navigate(Screen.Payment.createRoute(orderId)) {
                        popUpTo(Screen.Checkout.route) { inclusive = true }
                    }
                }
            )
        }

        // Payment
        composable(
            route = Screen.Payment.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            PaymentScreen(
                orderId = orderId,
                onPaymentResult = { success ->
                    navController.navigate(Screen.PaymentResult.createRoute(orderId, success)) {
                        popUpTo(Screen.Payment.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.PaymentResult.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType },
                navArgument("success") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val success = backStackEntry.arguments?.getBoolean("success") ?: false
            PaymentResultScreen(
                orderId = orderId,
                success = success,
                onViewOrder = {
                    navController.navigate(Screen.OrderDetail.createRoute(orderId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBackHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Orders
        composable(Screen.Orders.route) {
            OrderListScreen(
                onOrderClick = { orderId ->
                    navController.navigate(Screen.OrderDetail.createRoute(orderId))
                }
            )
        }

        composable(
            route = Screen.OrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(
                orderId = orderId,
                onNavigateBack = { navController.popBackStack() },
                onDownload = { productId ->
                    navController.navigate(Screen.DownloadList.route)
                }
            )
        }

        // Downloads
        composable(Screen.DownloadList.route) {
            DownloadListScreen(
                onNavigateToOffline = { navController.navigate(Screen.OfflineFiles.route) }
            )
        }

        composable(Screen.OfflineFiles.route) {
            OfflineFilesScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Profile
        composable(Screen.Profile.route) {
            ProfileScreen(
                onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onOrders = { navController.navigate(Screen.Orders.route) },
                onDownloads = { navController.navigate(Screen.DownloadList.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    kotlinx.coroutines.runBlocking {
                        appContainer.authRepository.logout()
                    }
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onSecuritySettings = { navController.navigate(Screen.SecuritySettings.route) },
                onAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(Screen.SecuritySettings.route) {
            SecuritySettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.About.route) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
