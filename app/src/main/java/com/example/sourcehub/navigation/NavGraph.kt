package com.example.sourcehub.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.runBlocking

// Routes where the bottom nav bar should be visible
private val bottomNavRoutes = setOf(
    Screen.Home.route, Screen.Search.route, Screen.Cart.route,
    Screen.Orders.route, Screen.Profile.route
)

@Composable
fun NavGraph() {
    val appContainer = remember { SourcehubApplication.instance.appContainer }
    val isLoggedIn by appContainer.tokenManager.isLoggedIn.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route

    // React to login state changes
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
        } else {
            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Auth ──
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Home ──
            composable(Screen.Home.route) {
                HomeScreen(
                    onProductClick = { pid -> navController.navigate(Screen.ProductDetail.createRoute(pid)) },
                    onCategoryClick = { cid, cname -> navController.navigate(Screen.ProductList.createRoute(cid, cname)) },
                    onBannerClick = { banner ->
                        when (banner.linkType.name) {
                            "PRODUCT" -> navController.navigate(Screen.ProductDetail.createRoute(banner.linkValue))
                            "CATEGORY" -> navController.navigate(Screen.ProductList.createRoute(banner.linkValue, banner.title))
                        }
                    }
                )
            }

            // ── Search ──
            composable(Screen.Search.route) {
                SearchScreen(onProductClick = { pid -> navController.navigate(Screen.ProductDetail.createRoute(pid)) })
            }

            // ── Product ──
            composable(
                route = Screen.ProductList.route,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("categoryName") { type = NavType.StringType; defaultValue = "全部商品" }
                )
            ) { entry ->
                ProductListScreen(
                    categoryId = entry.arguments?.getString("categoryId") ?: "",
                    categoryName = entry.arguments?.getString("categoryName") ?: "全部商品",
                    onProductClick = { pid -> navController.navigate(Screen.ProductDetail.createRoute(pid)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { entry ->
                ProductDetailScreen(
                    productId = entry.arguments?.getString("productId") ?: "",
                    onNavigateBack = { navController.popBackStack() },
                    onPreview = { navController.navigate(Screen.ProductPreview.createRoute(entry.arguments?.getString("productId") ?: "")) },
                    onBuyNow = { orderId -> navController.navigate(Screen.Payment.createRoute(orderId)) },
                    onAddToCartSuccess = {}
                )
            }

            composable(
                route = Screen.ProductPreview.route,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { entry ->
                ProductPreviewScreen(
                    productId = entry.arguments?.getString("productId") ?: "",
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Cart ──
            composable(Screen.Cart.route) {
                CartScreen(
                    onCheckout = { navController.navigate(Screen.Checkout.route) },
                    onProductClick = { pid -> navController.navigate(Screen.ProductDetail.createRoute(pid)) }
                )
            }

            // ── Checkout ──
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

            // ── Payment ──
            composable(
                route = Screen.Payment.route,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                PaymentScreen(
                    orderId = entry.arguments?.getString("orderId") ?: "",
                    onPaymentResult = { success ->
                        navController.navigate(Screen.PaymentResult.createRoute(entry.arguments?.getString("orderId") ?: "", success)) {
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
            ) { entry ->
                PaymentResultScreen(
                    orderId = entry.arguments?.getString("orderId") ?: "",
                    success = entry.arguments?.getBoolean("success") ?: false,
                    onViewOrder = {
                        navController.navigate(Screen.OrderDetail.createRoute(entry.arguments?.getString("orderId") ?: "")) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBackHome = {
                        navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            // ── Orders ──
            composable(Screen.Orders.route) {
                OrderListScreen(onOrderClick = { oid -> navController.navigate(Screen.OrderDetail.createRoute(oid)) })
            }

            composable(
                route = Screen.OrderDetail.route,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                OrderDetailScreen(
                    orderId = entry.arguments?.getString("orderId") ?: "",
                    onNavigateBack = { navController.popBackStack() },
                    onDownload = { navController.navigate(Screen.DownloadList.route) }
                )
            }

            // ── Downloads ──
            composable(Screen.DownloadList.route) {
                DownloadListScreen(onNavigateToOffline = { navController.navigate(Screen.OfflineFiles.route) })
            }

            composable(Screen.OfflineFiles.route) {
                OfflineFilesScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Profile ──
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onOrders = { navController.navigate(Screen.Orders.route) },
                    onDownloads = { navController.navigate(Screen.DownloadList.route) },
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onLogout = {
                        runBlocking { appContainer.authRepository.logout() }
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            composable(Screen.EditProfile.route) {
                EditProfileScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Settings ──
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
}
