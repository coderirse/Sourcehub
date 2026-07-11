package com.example.sourcehub.presentation.payment

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

data class PaymentUiState(
    val order: Order? = null,
    val selectedMethod: PaymentMethod = PaymentMethod.WECHAT,
    val isProcessing: Boolean = false,
    val isLoading: Boolean = false,
    val paymentResult: PaymentResult? = null
)

class PaymentViewModel : ViewModel() {
    private val appContainer = SourcehubApplication.instance.appContainer
    private val orderRepository = appContainer.orderRepository
    private val paymentRepository = appContainer.paymentRepository
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

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

    fun selectMethod(method: PaymentMethod) { _uiState.update { it.copy(selectedMethod = method) } }

    fun processPayment() {
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
