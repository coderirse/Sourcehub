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

data class OrderDetailUiState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class OrderDetailViewModel : ViewModel() {
    private val appContainer = SourcehubApplication.instance.appContainer
    private val orderRepository = appContainer.orderRepository
    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

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

    fun startDownload(productId: String) {
        viewModelScope.launch {
            val userId = appContainer.authRepository.getUserId()
            val orderId = _uiState.value.order?.id ?: return@launch
            appContainer.downloadRepository.startDownload(userId, orderId, productId)
        }
    }
}
