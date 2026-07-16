/**
 * 商品详情页面的视图模型和界面状态。
 *
 * [ProductDetailViewModel] 通过 ID 加载单个商品，支持"加入购物车"
 * 和"立即购买"操作，并管理界面自动关闭的"已加入购物车"临时确认消息。
 */
package com.example.sourcehub.presentation.product.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.Product
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [ProductDetailScreen] 的不可变状态快照。
 *
 * @property product 已加载的商品详情，首次成功加载前为 null。
 * @property isLoading 商品详情当前是否正在获取中。
 * @property error 获取失败时的错误消息。
 * @property isAddingToCart 加入购物车请求是否正在进行中（用于禁用按钮）。
 * @property cartAddedMessage 加入购物车后在 snackbar 中显示的临时确认消息。
 */
data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAddingToCart: Boolean = false,
    val cartAddedMessage: String? = null
)

/**
 * 驱动商品详情页面的视图模型。
 *
 * 提供 [loadProduct]、[addToCart]、[buyNow] 和 [clearCartMessage] 操作。
 * 所有仓库调用在 [viewModelScope] 上运行，并以响应方式更新 [uiState]。
 */
class ProductDetailViewModel : ViewModel() {
    /** 应用程序级依赖容器。 */
    private val appContainer = SourcehubApplication.instance.appContainer
    /** 用于获取商品详情的仓库。 */
    private val productRepository = appContainer.productRepository
    /** 用于购物车操作的仓库。 */
    private val cartRepository = appContainer.cartRepository

    /** [uiState] 的后端可变流。 */
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    /** [ProductDetailScreen] 观察的只读状态流。 */
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    /**
     * 加载给定 [productId] 的商品详情。
     *
     * 获取时将 [ProductDetailUiState.isLoading] 设为 true，
     * 完成后填充 [ProductDetailUiState.product] 或 [ProductDetailUiState.error]。
     */
    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = productRepository.getProductDetail(productId)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, product = result.data) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 将当前加载的商品加入用户购物车。
     *
     * 从 [AuthRepository] 读取当前用户 ID，然后调用 [CartRepository.addToCart]。
     * 请求进行中将 [ProductDetailUiState.isAddingToCart] 设为 true（界面将禁用按钮），
     * 成功后将 [ProductDetailUiState.cartAddedMessage] 设为确认消息。
     */
    fun addToCart() {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingToCart = true) }
            cartRepository.addToCart(
                userId = appContainer.authRepository.getUserId(),
                productId = product.id,
                productTitle = product.title,
                productCover = product.coverUrl,
                price = product.price
            )
            _uiState.update { it.copy(isAddingToCart = false, cartAddedMessage = "已加入购物车") }
        }
    }

    /**
     * 为当前加载的商品创建立即购买订单。
     *
     * 调用 [OrderRepository.createOrder]，传入包含当前商品的单项列表（数量为 1）。
     * 成功后调用 [onOrderCreated] 并传入新订单 ID，以便调用方导航到订单页面。
     *
     * @param onOrderCreated 成功时调用，参数为新订单 ID。
     */
    fun buyNow(onOrderCreated: (String) -> Unit) {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            val result = appContainer.orderRepository.createOrder(
                userId = appContainer.authRepository.getUserId(),
                // 单项订单：当前商品一件。
                items = listOf(product.id to 1)
            )
            when (result) {
                is Resource.Success -> onOrderCreated(result.data.id)
                else -> {}
            }
        }
    }

    /** 清除临时购物车确认消息，通常在 snackbar 关闭后调用。 */
    fun clearCartMessage() { _uiState.update { it.copy(cartAddedMessage = null) } }
}
