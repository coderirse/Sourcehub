package com.example.sourcehub.presentation.payment

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

data class PaymentResultUiState(val order: Order? = null)

class PaymentResultViewModel : ViewModel() {
    private val orderRepository = SourcehubApplication.instance.appContainer.orderRepository
    private val _uiState = MutableStateFlow(PaymentResultUiState())
    val uiState: StateFlow<PaymentResultUiState> = _uiState.asStateFlow()

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            when (val result = orderRepository.getOrderDetail(orderId)) {
                is Resource.Success -> _uiState.update { it.copy(order = result.data) }
                else -> {}
            }
        }
    }
}
