package com.example.sourcehub.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.Product
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val products: List<Product> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isSearching: Boolean = false
)

class SearchViewModel : ViewModel() {
    private val productRepository = SourcehubApplication.instance.appContainer.productRepository
    private val prefsManager = SourcehubApplication.instance.appContainer.preferencesManager
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init { loadRecentSearches() }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            prefsManager.recentSearchesFlow.collect { searches ->
                _uiState.update { it.copy(recentSearches = searches) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(products = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _uiState.update { it.copy(isSearching = true) }
            when (val result = productRepository.searchProducts(query)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSearching = false, products = result.data) }
                    prefsManager.addRecentSearch(query)
                }
                is Resource.Error -> _uiState.update { it.copy(isSearching = false) }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearQuery() {
        _uiState.update { it.copy(query = "", products = emptyList()) }
        searchJob?.cancel()
    }
}
