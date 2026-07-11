package com.example.sourcehub.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Auth
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")

    // Main Tabs
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Cart : Screen("cart")
    data object Orders : Screen("orders")
    data object Profile : Screen("profile")

    // Detail Screens
    data object ProductList : Screen("product_list?categoryId={categoryId}&categoryName={categoryName}") {
        fun createRoute(categoryId: String = "", categoryName: String = "") =
            "product_list?categoryId=$categoryId&categoryName=$categoryName"
    }

    data object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }

    data object ProductPreview : Screen("product_preview/{productId}") {
        fun createRoute(productId: String) = "product_preview/$productId"
    }

    data object Checkout : Screen("checkout")
    data object Payment : Screen("payment/{orderId}") {
        fun createRoute(orderId: String) = "payment/$orderId"
    }

    data object PaymentResult : Screen("payment_result/{orderId}/{success}") {
        fun createRoute(orderId: String, success: Boolean) =
            "payment_result/$orderId/$success"
    }

    data object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }

    data object DownloadList : Screen("download_list")
    data object OfflineFiles : Screen("offline_files")
    data object EditProfile : Screen("edit_profile")
    data object Settings : Screen("settings")
    data object SecuritySettings : Screen("security_settings")
    data object About : Screen("about")
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("首页", Icons.Default.Home, Screen.Home.route),
    BottomNavItem("搜索", Icons.Default.Search, Screen.Search.route),
    BottomNavItem("购物车", Icons.Default.ShoppingCart, Screen.Cart.route),
    BottomNavItem("订单", Icons.Default.Receipt, Screen.Orders.route),
    BottomNavItem("我的", Icons.Default.Person, Screen.Profile.route)
)
