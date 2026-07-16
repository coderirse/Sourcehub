/**
 * 商品预览页面的视图模型和界面状态。
 *
 * [ProductPreviewViewModel] 从商品仓库加载商品的标题、描述和页数。
 * 这些元数据字段由 [ProductPreviewScreen] 用于动态生成占位预览位图。
 */
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

/**
 * [ProductPreviewScreen] 的不可变状态快照。
 *
 * @property productTitle 商品标题，用于生成的预览页面页眉。
 * @property productDescription 商品描述，跨预览页面摘录显示。
 * @property pageCount 商品总页数（界面中上限为 [PREVIEW_MAX_PAGES]）。
 * @property isLoading 预览元数据当前是否正在获取中。
 * @property error 获取失败时的错误消息。
 */
data class ProductPreviewUiState(
    val productTitle: String = "",
    val productDescription: String = "",
    val pageCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 驱动商品预览页面的视图模型。
 *
 * 通过 [ProductRepository.getProductDetail] 获取商品元数据，
 * 仅提取与预览生成相关的字段（标题、描述、页数）。
 */
class ProductPreviewViewModel : ViewModel() {
    /** 用于获取商品元数据的仓库。 */
    private val productRepository = SourcehubApplication.instance.appContainer.productRepository

    /** [uiState] 的后端可变流。 */
    private val _uiState = MutableStateFlow(ProductPreviewUiState())
    /** [ProductPreviewScreen] 观察的只读状态流。 */
    val uiState: StateFlow<ProductPreviewUiState> = _uiState.asStateFlow()

    /**
     * 加载由 [productId] 标识的商品的预览元数据。
     *
     * 成功时填充 [ProductPreviewUiState.productTitle]、
     * [ProductPreviewUiState.productDescription] 和 [ProductPreviewUiState.pageCount]。
     * 失败时设置 [ProductPreviewUiState.error]。
     */
    fun loadPreview(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = productRepository.getProductDetail(productId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        productTitle = result.data.title,
                        productDescription = result.data.description,
                        pageCount = result.data.pageCount
                    )
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> {}
            }
        }
    }
}
