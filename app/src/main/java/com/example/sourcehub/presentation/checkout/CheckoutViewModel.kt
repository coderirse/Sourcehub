package com.example.sourcehub.presentation.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val couponCode: String = "",
    val discountAmount: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdOrderId: String? = null
)

class CheckoutViewModel : ViewModel() {
    private val appContainer = SourcehubApplication.instance.appContainer
    private val orderRepository = appContainer.orderRepository
    private val cartRepository = appContainer.cartRepository
    private val userId = appContainer.authRepository.getUserId()
    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    fun onCouponChange(code: String) { _uiState.update { it.copy(couponCode = code, discountAmount = 0.0) } }

    fun applyCoupon() {
        if (_uiState.value.couponCode == "SAVE10") {
            _uiState.update { it.copy(discountAmount = 0.1, error = null) }
        } else if (_uiState.value.couponCode.isNotEmpty()) {
            _uiState.update { it.copy(error = "优惠码无效") }
        }
    }

    fun placeOrder(cartItems: List<com.example.sourcehub.domain.model.CartItem>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val items = cartItems.map { it.productId to it.quantity }
            val cpCode = _uiState.value.couponCode
            when (val result = orderRepository.createOrder(userId, items, cpCode)) {
                is Resource.Success -> {
                    cartRepository.clearCart(userId)
                    _uiState.update { it.copy(isLoading = false, createdOrderId = result.data.id) }
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> {}
            }
        }
    }
}
