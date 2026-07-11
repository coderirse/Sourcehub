package com.example.sourcehub.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.Banner
import com.example.sourcehub.domain.model.Category
import com.example.sourcehub.domain.model.Product
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val banners: List<Banner> = emptyList(),
    val categories: List<Category> = emptyList(),
    val recommendedProducts: List<Product> = emptyList(),
    val newArrivals: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val productRepository = SourcehubApplication.instance.appContainer.productRepository
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load banners
            when (val banners = productRepository.getBanners()) {
                is Resource.Success -> _uiState.update { it.copy(banners = banners.data) }
                is Resource.Error -> _uiState.update { it.copy(error = banners.message) }
                is Resource.Loading -> {}
            }

            // Load categories
            when (val categories = productRepository.getCategories()) {
                is Resource.Success -> _uiState.update { it.copy(categories = categories.data) }
                else -> {}
            }

            // Load recommended
            when (val recommended = productRepository.getRecommendedProducts()) {
                is Resource.Success -> _uiState.update { it.copy(recommendedProducts = recommended.data) }
                else -> {}
            }

            // Load new arrivals
            when (val newArrivals = productRepository.getNewArrivals()) {
                is Resource.Success -> _uiState.update { it.copy(newArrivals = newArrivals.data) }
                else -> {}
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
