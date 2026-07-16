/**
 * SourceHub 应用的 Jetpack Compose 导航图。
 *
 * 此文件定义了完整的导航拓扑：
 * - 单个 [NavHost]，包含每个屏幕作为 `composable()` 目的地。
 * - 在五个主导航标签（首页、搜索、购物车、订单、我的）上显示 Material 3 [NavigationBar]，
 *   在详情/认证/设置屏幕上隐藏。
 * - 响应 [TokenManager.isLoggedIn] 的认证守卫：用户登录时，
 *   导航图导航到 [Screen.Home] 并清除返回栈；退出登录时
 *   导航到 [Screen.Login]。
 *
 * ## 使用的导航模式
 * - **`popUpTo(0) { inclusive = true }`** ——登录/退出登录后使用，清除整个
 *   返回栈，防止用户在认证后按返回键回到登录屏幕。
 * - **`launchSingleTop = true` + `restoreState = true`** ——在底部导航
 *   中使用，避免创建同一标签的重复实例，并在切换回之前访问过的
 *   标签时恢复滚动位置。
 * - **`popUpTo(route) { inclusive = true }`** ——在结算→支付和
 *   支付→结果页面跳转后使用，从返回栈中移除中间屏幕。
 */
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

/**
 * 底部导航栏应可见的路由集合。
 *
 * 任何不在此集合中的路由将在没有 [NavigationBar] 的情况下渲染——
 * 这包括详情屏幕、认证流程、结算/支付和设置子屏幕。
 */
private val bottomNavRoutes = setOf(
    Screen.Home.route, Screen.Search.route, Screen.Cart.route,
    Screen.Orders.route, Screen.Profile.route
)

/**
 * 根 Composable 函数，连接整个导航图。
 *
 * ## 职责
 * 1. **认证守卫**：观察 [TokenManager.isLoggedIn]，每当登录状态变化时
 *    将用户导航到 [Screen.Home] 或 [Screen.Login]。
 * 2. **底部栏可见性**：仅在 [bottomNavRoutes] 中列出的路由上显示 [NavigationBar]。
 * 3. **NavHost**：声明每个 `composable()` 目的地及其屏幕 Composable、
 *    参数定义和导航回调。
 *
 * 该函数是自包含的——调用者只需将 [NavGraph] 放置在 [SourcehubTheme] 内，
 * 图会内部处理所有路由。
 */
@Composable
fun NavGraph() {
    val appContainer = remember { SourcehubApplication.instance.appContainer }

    // 响应式观察登录状态。当令牌存储变化时（登录、退出登录、
    // 令牌过期），[isLoggedIn] 会发射新值，下方的 [LaunchedEffect] 会做出响应。
    val isLoggedIn by appContainer.tokenManager.isLoggedIn.collectAsStateWithLifecycle()

    val navController = rememberNavController()

    // 从返回栈条目推导当前路由，以便高亮正确的底部导航项并决定是否显示底栏。
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    // 在组合时确定起始目的地。此值仅在初始组合时使用；
    // 之后由 [LaunchedEffect] 处理重定向。
    val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route

    // 认证守卫：在运行时响应登录状态变化。
    // [popUpTo(0) { inclusive = true }] 清除整个返回栈，因此用户
    // 不能按返回键回到过期的屏幕（例如登录后按返回键
    // 否则会进入空白状态）。
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
                                        // [saveState] + [restoreState] 在用户返回之前访问过的标签时保留滚动位置。
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
            // ═══════════════════════════════════════════════════════════════════
            // 认证流程
            // ═══════════════════════════════════════════════════════════════════

            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    onLoginSuccess = {
                        // 成功时清除返回栈，防止用户按返回键回到登录界面。
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

            // ═══════════════════════════════════════════════════════════════════
            // 首页（仪表盘）
            // ═══════════════════════════════════════════════════════════════════

            composable(Screen.Home.route) {
                HomeScreen(
                    onProductClick = { pid -> navController.navigate(Screen.ProductDetail.createRoute(pid)) },
                    onCategoryClick = { cid, cname -> navController.navigate(Screen.ProductList.createRoute(cid, cname)) },
                    onBannerClick = { banner ->
                        // Banner 可以链接到商品详情或分类列表。
                        // 链接类型由后端确定，并在此映射到正确的导航目的地。
                        when (banner.linkType.name) {
                            "PRODUCT" -> navController.navigate(Screen.ProductDetail.createRoute(banner.linkValue))
                            "CATEGORY" -> navController.navigate(Screen.ProductList.createRoute(banner.linkValue, banner.title))
                        }
                    }
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // 搜索
            // ═══════════════════════════════════════════════════════════════════

            composable(Screen.Search.route) {
                SearchScreen(onProductClick = { pid -> navController.navigate(Screen.ProductDetail.createRoute(pid)) })
            }

            // ═══════════════════════════════════════════════════════════════════
            // 商品流程
            // ═══════════════════════════════════════════════════════════════════

            // 商品列表（可按分类筛选）。
            // 默认参数值允许导航到"全部商品"而无需显式传递 categoryId/categoryName。
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

            // ═══════════════════════════════════════════════════════════════════
            // 购物车
            // ═══════════════════════════════════════════════════════════════════

            composable(Screen.Cart.route) {
                CartScreen(
                    onCheckout = { navController.navigate(Screen.Checkout.route) },
                    onProductClick = { pid -> navController.navigate(Screen.ProductDetail.createRoute(pid)) }
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // 结算 → 支付流程
            // ═══════════════════════════════════════════════════════════════════

            composable(Screen.Checkout.route) {
                CheckoutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPayment = { orderId ->
                        // 跳转到支付页时从返回栈中移除结算页，
                        // 防止用户返回并重新提交订单。
                        navController.navigate(Screen.Payment.createRoute(orderId)) {
                            popUpTo(Screen.Checkout.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Payment.route,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                PaymentScreen(
                    orderId = entry.arguments?.getString("orderId") ?: "",
                    onPaymentResult = { success ->
                        // 用 PaymentResult 替换 Payment，从返回栈中移除 Payment，
                        // 防止用户按返回键重新尝试支付。
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
                        // 导航到订单详情，弹出到首页，使返回栈干净——
                        // 用户不会返回到 PaymentResult。
                        navController.navigate(Screen.OrderDetail.createRoute(entry.arguments?.getString("orderId") ?: "")) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBackHome = {
                        navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // 订单
            // ═══════════════════════════════════════════════════════════════════

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

            // ═══════════════════════════════════════════════════════════════════
            // 下载
            // ═══════════════════════════════════════════════════════════════════

            composable(Screen.DownloadList.route) {
                DownloadListScreen(onNavigateToOffline = { navController.navigate(Screen.OfflineFiles.route) })
            }

            composable(Screen.OfflineFiles.route) {
                OfflineFilesScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ═══════════════════════════════════════════════════════════════════
            // 我的
            // ═══════════════════════════════════════════════════════════════════

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onOrders = { navController.navigate(Screen.Orders.route) },
                    onDownloads = { navController.navigate(Screen.DownloadList.route) },
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onLogout = {
                        // 此处使用 [runBlocking] 是可接受的，因为退出登录是
                        // 快速的内存操作（清除 SharedPreferences）。
                        // 在包含网络调用的生产应用中，这将是在
                        // ViewModel 作用域中启动的协程。
                        runBlocking { appContainer.authRepository.logout() }
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            composable(Screen.EditProfile.route) {
                EditProfileScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ═══════════════════════════════════════════════════════════════════
            // 设置
            // ═══════════════════════════════════════════════════════════════════

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
