package com.example.sourcehub.presentation.product.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.Product
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAddingToCart: Boolean = false,
    val cartAddedMessage: String? = null
)

class ProductDetailViewModel : ViewModel() {
    private val appContainer = SourcehubApplication.instance.appContainer
    private val productRepository = appContainer.productRepository
    private val cartRepository = appContainer.cartRepository
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = productRepository.getProductDetail(productId)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, product = result.data) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun addToCart() {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingToCart = true) }
            cartRepository.addToCart(
                userId = appContainer.authRepository.getUserId(),
                productId = product.id,
                productTitle = product.title,
                productCover = product.coverUrl,
                price = product.price
            )
            _uiState.update { it.copy(isAddingToCart = false, cartAddedMessage = "已加入购物车") }
        }
    }

    fun buyNow(onOrderCreated: (String) -> Unit) {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            val result = appContainer.orderRepository.createOrder(
                userId = appContainer.authRepository.getUserId(),
                items = listOf(product.id to 1)
            )
            when (result) {
                is Resource.Success -> onOrderCreated(result.data.id)
                else -> {}
            }
        }
    }

    fun clearCartMessage() { _uiState.update { it.copy(cartAddedMessage = null) } }
}
