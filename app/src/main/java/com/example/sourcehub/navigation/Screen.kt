/**
 * SourceHub 应用的导航路由定义。
 *
 * 此文件定义：
 * - [Screen]：一个密封类层次结构，每个 `data object` 代表 Jetpack Compose
 *   导航图中的一个独立目的地。带路径参数的路由暴露 `createRoute()`
 *   工厂函数以构建类型安全的 URI。
 * - [BottomNavItem]：一个简单的数据类，将底部导航标签文本、图标和路由配对。
 * - [bottomNavItems]：底部导航栏中显示的有序标签列表。
 *
 * 使用密封类确保每个路由都按名称引用（无字符串类型的导航），
 * 并且编译器会在添加新目的地时标记缺失的分支。
 */
package com.example.sourcehub.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 应用中每个导航目的地的类型安全表示。
 *
 * 每个 `data object` 是一个单例路由。携带参数的路由使用
 * Navigation-Compose 参数占位符语法（`{arg}`），并提供伴生的
 * `createRoute()` 函数，使调用者无需手动拼接字符串。
 *
 * @property route Navigation-Compose 路由模式字符串（如 `"product_detail/{productId}"`）。
 */
sealed class Screen(val route: String) {

    // ── 认证流程 ─────────────────────────────────────────────────────────────────

    /** 登录/注册屏幕。未认证用户的入口点。 */
    data object Login : Screen("login")

    /** 新用户注册屏幕。 */
    data object Register : Screen("register")

    /** 密码重置请求屏幕。 */
    data object ForgotPassword : Screen("forgot_password")

    // ── 主要底部导航标签 ───────────────────────────────────────────────────────

    /** 带 Banner、分类和推荐商品的首页仪表盘。 */
    data object Home : Screen("home")

    /** 带历史记录的全文本商品搜索。 */
    data object Search : Screen("search")

    /** 带数量管理和结算按钮的购物车。 */
    data object Cart : Screen("cart")

    /** 订单历史列表。 */
    data object Orders : Screen("orders")

    /** 用户资料、账户设置和退出登录。 */
    data object Profile : Screen("profile")

    // ── 商品流程 ──────────────────────────────────────────────────────────────

    /**
     * 按分类筛选的商品列表。
     * 路由模式包含可选查询参数，因此屏幕也可以在 [categoryId] 为空时
     * 显示"全部商品"。
     */
    data object ProductList : Screen("product_list?categoryId={categoryId}&categoryName={categoryName}") {
        fun createRoute(categoryId: String = "", categoryName: String = "") =
            "product_list?categoryId=$categoryId&categoryName=$categoryName"
    }

    /** 以 [productId] 为键的单个商品详情视图。 */
    data object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }

    /** 数字产品的应用内 PDF/文档预览。 */
    data object ProductPreview : Screen("product_preview/{productId}") {
        fun createRoute(productId: String) = "product_preview/$productId"
    }

    // ── 购买流程 ──────────────────────────────────────────────────────────────

    /** 支付前的购物车确认和地址选择屏幕。 */
    data object Checkout : Screen("checkout")

    /** 给定 [orderId] 的支付网关屏幕。 */
    data object Payment : Screen("payment/{orderId}") {
        fun createRoute(orderId: String) = "payment/$orderId"
    }

    /** 显示给定 [orderId] 的成功/失败结果的支付后结果屏幕。 */
    data object PaymentResult : Screen("payment_result/{orderId}/{success}") {
        fun createRoute(orderId: String, success: Boolean) =
            "payment_result/$orderId/$success"
    }

    /** 订单详情视图。也可从支付结果屏幕跳转。 */
    data object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }

    // ── 下载 ─────────────────────────────────────────────────────────────────

    /** 活跃和已完成的下载列表。 */
    data object DownloadList : Screen("download_list")

    /** 设备上存储的可离线访问文件（已加密）。 */
    data object OfflineFiles : Screen("offline_files")

    // ── 个人资料子屏幕 ───────────────────────────────────────────────────────

    /** 编辑个人资料（显示名称、头像等）。 */
    data object EditProfile : Screen("edit_profile")

    /** 应用全局设置（主题、语言、远程 API 切换等）。 */
    data object Settings : Screen("settings")

    /** 安全特定设置（生物识别锁、PIN 码、会话管理）。 */
    data object SecuritySettings : Screen("security_settings")

    /** 应用版本、许可证和致谢。 */
    data object About : Screen("about")
}

/**
 * 描述 Material 3 底部导航栏中的单个标签。
 *
 * @property label 图标下方显示的中文本地化标签。
 * @property icon  标签的 Material Icons [ImageVector]。
 * @property route 此标签导航到的 Navigation-Compose 路由（必须匹配 [Screen]）。
 */
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

/**
 * 底部导航标签的有序列表。
 *
 * 此处的顺序直接映射到 [NavigationBar] 中从左到右的顺序。
 * 每个标签的 [route] 必须是 [NavGraph] 中 [bottomNavRoutes] 的成员，
 * 以便底栏在正确的屏幕上可见。
 */
val bottomNavItems = listOf(
    BottomNavItem("首页", Icons.Default.Home, Screen.Home.route),
    BottomNavItem("搜索", Icons.Default.Search, Screen.Search.route),
    BottomNavItem("购物车", Icons.Default.ShoppingCart, Screen.Cart.route),
    BottomNavItem("订单", Icons.Default.Receipt, Screen.Orders.route),
    BottomNavItem("我的", Icons.Default.Person, Screen.Profile.route)
)
