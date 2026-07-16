package com.example.sourcehub.presentation.orders.detail

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
 * 订单详情页面的 UI 状态。
 *
 * @property order 加载成功的订单数据，null 表示尚未加载或加载失败。
 * @property isLoading 是否正在从仓库加载订单详情。
 * @property error 加载失败时的错误消息，成功时为 null。
 */
data class OrderDetailUiState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 订单详情页面的 ViewModel。
 *
 * 负责加载指定订单的详细信息（通过 [OrderRepository.getOrderDetail]），
 * 并提供对已支付订单中商品的下载发起能力（通过 [DownloadRepository.startDownload]）。
 * 状态通过 [OrderDetailUiState] 以 [StateFlow] 向 UI 层暴露。
 */
class OrderDetailViewModel : ViewModel() {
    /** 应用级依赖容器，提供各仓库实例。 */
    private val appContainer = SourcehubApplication.instance.appContainer
    /** 订单仓库，用于拉取单个订单详情。 */
    private val orderRepository = appContainer.orderRepository

    /** 内部可变状态流。 */
    private val _uiState = MutableStateFlow(OrderDetailUiState())
    /** 向 UI 暴露的只读状态流。 */
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    /**
     * 根据订单 ID 加载订单详情。
     * 先设置 [OrderDetailUiState.isLoading] 为 true，
     * 然后根据仓库返回的 [Resource] 结果更新 UI 状态。
     *
     * @param orderId 要加载的订单 ID。
     */
    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = orderRepository.getOrderDetail(orderId)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, order = result.data) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 对指定商品发起下载任务。
     * 需要当前订单 ID 和用户 ID 作为下载任务的关联信息。
     * 若当前 UI 状态中订单为 null（未加载），则直接返回，不执行下载。
     *
     * @param productId 要下载的商品 ID。
     */
    fun startDownload(productId: String) {
        viewModelScope.launch {
            val userId = appContainer.authRepository.getUserId()
            // 仅在订单已加载时才允许发起下载
            val orderId = _uiState.value.order?.id ?: return@launch
            appContainer.downloadRepository.startDownload(userId, orderId, productId)
        }
    }
}
