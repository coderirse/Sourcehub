/**
 * 首页的视图模型和界面状态。
 *
 * [HomeViewModel] 在初始化时从 [ProductRepository] 获取横幅、分类、
 * 推荐商品和最新上架商品，并通过 [HomeScreen] 观察的单一
 * [HomeUiState] 数据类暴露它们。
 */
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

/**
 * [HomeScreen] 的不可变状态快照。
 *
 * @property banners 轮播中显示的推广横幅。
 * @property categories 以可水平滚动标签形式显示的商品分类。
 * @property recommendedProducts 精选的"热门推荐"商品。
 * @property newArrivals 最近添加的商品。
 * @property isLoading 初始数据加载是否正在进行中。
 * @property error 加载失败且无缓存数据可用时的错误消息。
 */
data class HomeUiState(
    val banners: List<Banner> = emptyList(),
    val categories: List<Category> = emptyList(),
    val recommendedProducts: List<Product> = emptyList(),
    val newArrivals: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 驱动首页的视图模型。
 *
 * 创建时立即调用 [loadData] 填充所有首页区域。
 * 每个区域（横幅、分类、推荐、最新上架）在单个协程中
 * 按顺序加载，以便每次调用完成后立即显示部分结果。
 */
class HomeViewModel : ViewModel() {
    /** 提供首页数据的仓库。 */
    private val productRepository = SourcehubApplication.instance.appContainer.productRepository

    /** [uiState] 的可变支持流。 */
    private val _uiState = MutableStateFlow(HomeUiState())
    /** 供 [HomeScreen] 观察的只读状态流。 */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadData() }

    /**
     * 从仓库获取所有首页数据。
     *
     * 开始时将 [HomeUiState.isLoading] 设为 true，结束时设为 false。
     * 各区域的失败独立处理 — 横幅错误通过
     * [HomeUiState.error] 显示，而分类/推荐/最新上架
     * 的错误被静默忽略，以便其余区域仍然渲染。
     */
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 加载横幅 — 失败会显示到界面，以便用户可以重试。
            when (val banners = productRepository.getBanners()) {
                is Resource.Success -> _uiState.update { it.copy(banners = banners.data) }
                is Resource.Error -> _uiState.update { it.copy(error = banners.message) }
                is Resource.Loading -> {}
            }

            // 加载分类 — 错误被忽略；标签将不会出现。
            when (val categories = productRepository.getCategories()) {
                is Resource.Success -> _uiState.update { it.copy(categories = categories.data) }
                else -> {}
            }

            // 加载推荐商品 — 错误被忽略；该区域将保持为空。
            when (val recommended = productRepository.getRecommendedProducts()) {
                is Resource.Success -> _uiState.update { it.copy(recommendedProducts = recommended.data) }
                else -> {}
            }

            // 加载最新上架 — 错误被忽略；该区域将保持为空。
            when (val newArrivals = productRepository.getNewArrivals()) {
                is Resource.Success -> _uiState.update { it.copy(newArrivals = newArrivals.data) }
                else -> {}
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
