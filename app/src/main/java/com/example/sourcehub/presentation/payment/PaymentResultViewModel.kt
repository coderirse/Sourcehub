package com.example.sourcehub.presentation.payment

/**
 * 支付结果模块界面层。
 *
 * 一个轻量级视图模型，在支付完成后加载完整订单详情，
 * 以便结果页面可以显示订单号和金额信息。
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.Order
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 支付结果页面界面状态的不可变快照。
 *
 * @property order  支付刚刚完成的已加载 [Order]；加载中或失败/未加载时为 null
 */
data class PaymentResultUiState(val order: Order? = null)

/**
 * 支付结果页面的视图模型。
 *
 * 通过ID获取订单详情并填充 [PaymentResultUiState.order]，
 * 以便页面可以显示可读的订单参考信息。
 */
class PaymentResultViewModel : ViewModel() {
    private val orderRepository = SourcehubApplication.instance.appContainer.orderRepository
    private val _uiState = MutableStateFlow(PaymentResultUiState())
    val uiState: StateFlow<PaymentResultUiState> = _uiState.asStateFlow()

    /**
     * 加载订单详情并更新 [PaymentResultUiState.order]。
     *
     * 失败时静默忽略 — 页面已通过
     * [com.example.sourcehub.presentation.payment.PaymentResultScreen.success]
     * 参数指示成功/失败。
     *
     * @param orderId  要获取的订单ID
     */
    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            when (val result = orderRepository.getOrderDetail(orderId)) {
                is Resource.Success -> _uiState.update { it.copy(order = result.data) }
                else -> { /* 静默忽略加载/错误 — 订单保持为 null */ }
            }
        }
    }
}
