/**
 * 分类筛选商品列表页面的视图模型和界面状态。
 *
 * [ProductListViewModel] 负责按分类加载商品（或分类为空时加载所有新品），
 * 并支持重试操作，该操作复用上一次请求的分类。
 */
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

/**
 * [ProductListScreen] 的不可变状态快照。
 *
 * @property products 当前展示的商品列表。
 * @property isLoading 商品获取是否正在进行中。
 * @property error 获取失败时的错误消息（成功时为 null）。
 */
data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 驱动商品列表页面的视图模型。
 *
 * 通过 [loadProducts] 根据给定的分类 ID 加载筛选后的商品。
 * 当 [categoryId] 为空时，改为获取所有新品商品（最多 50 个）。
 * 上一次请求的分类会被缓存，以便 [retry] 可以在调用方无需再次传递 ID 的情况下重复上次查询。
 */
class ProductListViewModel : ViewModel() {
    /** 用于获取商品数据的仓库。 */
    private val productRepository = SourcehubApplication.instance.appContainer.productRepository

    /** [uiState] 的后端可变流。 */
    private val _uiState = MutableStateFlow(ProductListUiState())
    /** [ProductListScreen] 观察的只读状态流。 */
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    /** 缓存的分类 ID，供 [retry] 重复上次请求使用。 */
    private var currentCategoryId = ""

    /**
     * 加载给定 [categoryId] 对应的商品。
     *
     * 如果 [categoryId] 为空，则获取最多 50 个新品作为默认的"全部商品"视图。
     * 在调用过程中会更新 [uiState] 的加载/错误/成功状态。
     *
     * @param categoryId 筛选所用的分类，或空字符串表示全部商品。
     */
    fun loadProducts(categoryId: String) {
        currentCategoryId = categoryId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // 未选择分类时，回退到新品列表作为默认展示。
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

    /**
     * 使用之前缓存的分类 ID 重试上一次 [loadProducts] 调用。
     * 通常由错误状态的界面调用。
     */
    fun retry() { loadProducts(currentCategoryId) }
}
