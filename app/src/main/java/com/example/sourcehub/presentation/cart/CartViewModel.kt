package com.example.sourcehub.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.CartItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = false
)

class CartViewModel : ViewModel() {
    private val appContainer = SourcehubApplication.instance.appContainer
    private val cartRepository = appContainer.cartRepository
    private val userId = appContainer.authRepository.getUserId()
    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cartRepository.getCartItems(userId).collect { items ->
                _uiState.update { state ->
                    state.copy(
                        items = items,
                        totalAmount = items.filter { it.id in state.selectedIds }.sumOf { it.price * it.quantity }
                    )
                }
            }
        }
    }

    fun toggleSelectAll() {
        _uiState.update { state ->
            if (state.selectedIds.size == state.items.size) {
                state.copy(selectedIds = emptySet(), totalAmount = 0.0)
            } else {
                val allIds = state.items.map { it.id }.toSet()
                state.copy(
                    selectedIds = allIds,
                    totalAmount = state.items.sumOf { it.price * it.quantity }
                )
            }
        }
    }

    fun toggleSelect(itemId: String) {
        _uiState.update { state ->
            val newSelected = state.selectedIds.toMutableSet()
            if (itemId in newSelected) newSelected.remove(itemId) else newSelected.add(itemId)
            state.copy(
                selectedIds = newSelected,
                totalAmount = state.items.filter { it.id in newSelected }.sumOf { it.price * it.quantity }
            )
        }
    }

    fun updateQuantity(itemId: String, quantity: Int) {
        viewModelScope.launch { cartRepository.updateQuantity(itemId, quantity) }
    }

    fun removeItem(itemId: String) {
        viewModelScope.launch {
            cartRepository.removeFromCart(itemId)
            _uiState.update { state ->
                val newSelected = state.selectedIds - itemId
                state.copy(selectedIds = newSelected)
            }
        }
    }

    fun getSelectedItems(): List<CartItem> {
        return _uiState.value.items.filter { it.id in _uiState.value.selectedIds }
    }
}
