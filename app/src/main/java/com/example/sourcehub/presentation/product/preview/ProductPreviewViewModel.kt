package com.example.sourcehub.presentation.product.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductPreviewUiState(
    val productTitle: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProductPreviewViewModel : ViewModel() {
    private val productRepository = SourcehubApplication.instance.appContainer.productRepository
    private val _uiState = MutableStateFlow(ProductPreviewUiState())
    val uiState: StateFlow<ProductPreviewUiState> = _uiState.asStateFlow()

    fun loadPreview(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = productRepository.getProductDetail(productId)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, productTitle = result.data.title) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> {}
            }
        }
    }
}
