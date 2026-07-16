package com.example.sourcehub.presentation.checkout

/**
 * 结算模块界面层。
 *
 * 管理订单确认流程：优惠码验证，通过
 * [com.example.sourcehub.domain.repository.OrderRepository] 创建订单，
 * 并将创建的订单ID传递给支付页面。
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 结算页面界面状态的不可变快照。
 *
 * @property couponCode      用户在优惠码输入框中输入的文本
 * @property discountAmount  有效优惠码激活时应用于小计的折扣乘数
 *                           （例如 0.1 = 九折）
 * @property isLoading       订单创建请求是否正在进行中
 * @property error           内联显示的可读错误消息
 * @property createdOrderId  订单创建后为非空；触发导航到支付页面
 */
data class CheckoutUiState(
    val couponCode: String = "",
    val discountAmount: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdOrderId: String? = null
)

/**
 * 结算/订单确认页面的视图模型。
 *
 * 验证硬编码的优惠码（"SAVE10" 可享九折），通过仓库创建订单，
 * 成功后清空购物车，并暴露生成的订单ID以便界面切换到支付页面。
 */
class CheckoutViewModel : ViewModel() {
    private val appContainer = SourcehubApplication.instance.appContainer
    private val orderRepository = appContainer.orderRepository
    private val cartRepository = appContainer.cartRepository
    private val userId = appContainer.authRepository.getUserId()
    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    /**
     * 更新优惠码文本字段。重置之前应用的任何折扣，
     * 因此用户必须在编辑后重新显式应用。
     */
    fun onCouponChange(code: String) { _uiState.update { it.copy(couponCode = code, discountAmount = 0.0) } }

    /**
     * 验证当前优惠码。"SAVE10" 可享九折；任何其他非空值显示通用错误消息。
     */
    fun applyCoupon() {
        if (_uiState.value.couponCode == "SAVE10") {
            _uiState.update { it.copy(discountAmount = 0.1, error = null) }
        } else if (_uiState.value.couponCode.isNotEmpty()) {
            _uiState.update { it.copy(error = "优惠码无效") }
        }
    }

    /**
     * 根据给定的购物车商品创建新订单。
     *
     * 成功后清空购物车并设置 [CheckoutUiState.createdOrderId]，
     * 界面观察该字段以触发导航到支付页面。失败时
     * [CheckoutUiState.error] 字段填充可读的错误消息。
     *
     * @param cartItems  订单中包含的商品（通常是整个购物车）
     */
    fun placeOrder(cartItems: List<com.example.sourcehub.domain.model.CartItem>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val items = cartItems.map { it.productId to it.quantity }
            val cpCode = _uiState.value.couponCode
            when (val result = orderRepository.createOrder(userId, items, cpCode)) {
                is Resource.Success -> {
                    // 订单已提交，清空购物车
                    cartRepository.clearCart(userId)
                    _uiState.update { it.copy(isLoading = false, createdOrderId = result.data.id) }
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> { /* 中间加载状态 — 无操作 */ }
            }
        }
    }
}
