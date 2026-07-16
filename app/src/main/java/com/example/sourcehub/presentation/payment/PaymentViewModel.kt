package com.example.sourcehub.presentation.payment

/**
 * 支付模块界面层。
 *
 * 管理支付流程：加载订单详情，跟踪已选的支付方式，
 * 并将实际支付委托给 [com.example.sourcehub.domain.repository.PaymentRepository]。
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.Order
import com.example.sourcehub.domain.model.PaymentMethod
import com.example.sourcehub.domain.model.PaymentResult
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 支付页面界面状态的不可变快照。
 *
 * @property order          正在支付的订单（加载完成前为 null）
 * @property selectedMethod 选中的支付方式单选按钮（默认：WECHAT）
 * @property isProcessing   支付请求是否正在进行中
 * @property isLoading      订单详情是否正在获取中
 * @property paymentResult  支付结果；被观察以触发结果页面导航
 */
data class PaymentUiState(
    val order: Order? = null,
    val selectedMethod: PaymentMethod = PaymentMethod.WECHAT,
    val isProcessing: Boolean = false,
    val isLoading: Boolean = false,
    val paymentResult: PaymentResult? = null
)

/**
 * 支付页面的视图模型。
 *
 * 通过ID加载 [Order]，更新选中的 [PaymentMethod]，
 * 并调用 [com.example.sourcehub.domain.repository.PaymentRepository.processPayment]
 * 来模拟支付处理。结果通过 [PaymentUiState.paymentResult] 暴露。
 */
class PaymentViewModel : ViewModel() {
    private val appContainer = SourcehubApplication.instance.appContainer
    private val orderRepository = appContainer.orderRepository
    private val paymentRepository = appContainer.paymentRepository
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    /**
     * 从仓库获取订单详情并存储到界面状态中。
     *
     * @param orderId  要加载的订单ID
     */
    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = orderRepository.getOrderDetail(orderId)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, order = result.data) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false) }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 更新单选按钮列表中显示的已选支付方式。
     *
     * @param method  用户点击的 [PaymentMethod]
     */
    fun selectMethod(method: PaymentMethod) { _uiState.update { it.copy(selectedMethod = method) } }

    /**
     * 对当前订单发起支付处理。
     *
     * 对空订单进行防护（提前返回）。调用支付仓库，
     * 并在完成时更新 [PaymentUiState.paymentResult]。
     */
    fun processPayment() {
        // 安全保护：如果订单尚未加载（理论上不可达），则直接退出
        val order = _uiState.value.order ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = paymentRepository.processPayment(
                orderId = order.id,
                amount = order.finalAmount,
                method = _uiState.value.selectedMethod
            )
            _uiState.update { it.copy(isProcessing = false, paymentResult = result) }
        }
    }
}
