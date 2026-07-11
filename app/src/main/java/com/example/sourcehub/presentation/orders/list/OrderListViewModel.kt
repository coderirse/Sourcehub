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

data class OrderListUiState(
    val orders: List<Order> = emptyList(),
    val selectedStatus: OrderStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val filteredOrders: List<Order> get() = if (selectedStatus != null) orders.filter { it.status == selectedStatus } else orders
}

class OrderListViewModel : ViewModel() {
    private val orderRepository = SourcehubApplication.instance.appContainer.orderRepository
    private val userId = SourcehubApplication.instance.appContainer.authRepository.getUserId()
    private val _uiState = MutableStateFlow(OrderListUiState())
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    init { loadOrders() }

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

    fun filterByStatus(status: OrderStatus?) { _uiState.update { it.copy(selectedStatus = status) } }
}
