package com.example.sourcehub.presentation.product.list

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

data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProductListViewModel : ViewModel() {
    private val productRepository = SourcehubApplication.instance.appContainer.productRepository
    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()
    private var currentCategoryId = ""

    fun loadProducts(categoryId: String) {
        currentCategoryId = categoryId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = if (categoryId.isEmpty()) {
                productRepository.getNewArrivals(50)
            } else {
                productRepository.getProductsByCategory(categoryId)
            }
            when (result) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, products = result.data) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun retry() { loadProducts(currentCategoryId) }
}
