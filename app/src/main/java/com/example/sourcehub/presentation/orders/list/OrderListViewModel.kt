package com.example.sourcehub.presentation.orders.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.Order
import com.example.sourcehub.domain.model.OrderStatus
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 订单列表页面的 UI 状态。
 *
 * @property orders 从仓库获取的原始订单列表。
 * @property selectedStatus 当前选中的筛选状态，null 表示"全部"（不筛选）。
 * @property isLoading 是否正在加载订单数据。
 * @property error 加载失败时的错误消息，成功时为 null。
 */
data class OrderListUiState(
    val orders: List<Order> = emptyList(),
    val selectedStatus: OrderStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 根据当前选中的状态筛选后的订单列表。
     * 如果 [selectedStatus] 为 null（全部），返回原始列表；
     * 否则只返回状态匹配的订单。
     */
    val filteredOrders: List<Order> get() = if (selectedStatus != null) orders.filter { it.status == selectedStatus } else orders
}

/**
 * 订单列表页面的 ViewModel。
 *
 * 负责从 [OrderRepository] 加载当前用户的订单数据，并支持按 [OrderStatus] 筛选。
 * 通过 [OrderListUiState] 以 [StateFlow] 的形式向 UI 层暴露状态。
 */
class OrderListViewModel : ViewModel() {
    /** 订单数据仓库实例。 */
    private val orderRepository = SourcehubApplication.instance.appContainer.orderRepository
    /** 当前登录用户的 ID。 */
    private val userId = SourcehubApplication.instance.appContainer.authRepository.getUserId()

    /** 内部可变的状态流，外部仅通过 [uiState] 读取。 */
    private val _uiState = MutableStateFlow(OrderListUiState())
    /** 向 UI 层暴露的只读状态流。 */
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    init { loadOrders() }

    /**
     * 加载当前用户的所有订单。
     * 从仓库获取数据后根据 [Resource] 结果更新状态：
     * - [Resource.Success]：填充订单列表并清除加载/错误状态。
     * - [Resource.Error]：记录错误消息。
     * - [Resource.Loading]：中间状态，不更新 UI（由调用侧的 isLoading 控制）。
     */
    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = orderRepository.getOrders(userId)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, orders = result.data) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 按订单状态筛选订单列表。
     *
     * @param status 要筛选的状态，传入 null 表示显示全部订单。
     */
    fun filterByStatus(status: OrderStatus?) { _uiState.update { it.copy(selectedStatus = status) } }
}
