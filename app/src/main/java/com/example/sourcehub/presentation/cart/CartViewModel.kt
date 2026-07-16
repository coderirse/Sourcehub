package com.example.sourcehub.presentation.cart

/**
 * 购物车模块界面层。
 *
 * 包含购物车页面组件及其视图模型，用于管理购物车状态：
 * 商品展示、选择切换、数量调整和移除。
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.CartItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 购物车页面界面状态的不可变快照。
 *
 * @property items        属于当前用户的所有购物车商品
 * @property selectedIds  当前已勾选商品的ID集合
 * @property totalAmount  所有*已选中*商品的 `价格 * 数量` 之和
 * @property isLoading    购物车数据是否正在获取/变更
 */
data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = false
)

/**
 * 购物车页面的视图模型。
 *
 * 观察当前用户的购物车仓库，暴露 [CartUiState] 流，
 * 并提供全选、切换选择、数量变更和商品移除等用户操作。
 * 所有变更通过应用容器的依赖图派发到购物车仓库。
 */
class CartViewModel : ViewModel() {
    private val appContainer = SourcehubApplication.instance.appContainer
    private val cartRepository = appContainer.cartRepository
    private val userId = appContainer.authRepository.getUserId()
    // 底层可变流；仅作为只读 StateFlow 暴露
    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        // 从仓库观察购物车商品，每次发射时更新界面状态
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

    /**
     * 切换全选：如果所有商品已选中则清除选择；否则选中所有商品并重新计算总计。
     */
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

    /**
     * 切换单个商品的选择状态并重新计算总计。
     *
     * @param itemId  被点击复选框的购物车商品ID
     */
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

    /**
     * 更新指定购物车商品的数量。仓库会将值限制为至少1（防止减到0或负数）。
     *
     * @param itemId   购物车商品ID
     * @param quantity  新的数量（可能被仓库限制）
     */
    fun updateQuantity(itemId: String, quantity: Int) {
        viewModelScope.launch { cartRepository.updateQuantity(itemId, quantity) }
    }

    /**
     * 从仓库中移除购物车商品，并从当前选择集合中删除其ID（使其不再计入总计）。
     *
     * @param itemId  要移除的购物车商品ID
     */
    fun removeItem(itemId: String) {
        viewModelScope.launch {
            cartRepository.removeFromCart(itemId)
            // 成功移除后清理选择集合
            _uiState.update { state ->
                val newSelected = state.selectedIds - itemId
                state.copy(selectedIds = newSelected)
            }
        }
    }

    /**
     * 返回当前选择集合中ID对应的 [CartItem] 列表。通常在导航到结算页面前调用。
     */
    fun getSelectedItems(): List<CartItem> {
        return _uiState.value.items.filter { it.id in _uiState.value.selectedIds }
    }
}
