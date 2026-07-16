/**
 * 搜索页面的视图模型和界面状态。
 *
 * [SearchViewModel] 在发出搜索请求前对用户输入进行 300 毫秒防抖处理，
 * 查询变更时取消进行中的搜索，并将成功的查询持久化到
 * [PreferencesManager] 中作为最近搜索历史。
 */
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

/**
 * [SearchScreen] 的不可变状态快照。
 *
 * @property query 搜索字段中的当前文本。
 * @property products 匹配当前查询的商品列表。
 * @property recentSearches 之前执行的搜索查询，查询为空时展示。
 * @property isSearching 防抖搜索请求当前是否正在进行中。
 */
data class SearchUiState(
    val query: String = "",
    val products: List<Product> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isSearching: Boolean = false
)

/**
 * 驱动搜索页面的视图模型。
 *
 * 处理查询防抖、搜索执行、最近搜索持久化以及查询清除。
 * 最近搜索在初始化时从 [PreferencesManager.recentSearchesFlow]
 * 以响应方式收集。
 */
class SearchViewModel : ViewModel() {
    /** 用于搜索商品的仓库。 */
    private val productRepository = SourcehubApplication.instance.appContainer.productRepository
    /** 用于持久化和观察最近搜索的偏好管理器。 */
    private val prefsManager = SourcehubApplication.instance.appContainer.preferencesManager

    /** [uiState] 的后端可变流。 */
    private val _uiState = MutableStateFlow(SearchUiState())
    /** [SearchScreen] 观察的只读状态流。 */
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * 当前正在执行的搜索协程的句柄。
     * 每次查询变更时取消，以实现防抖 — 只有最新的查询能够存活到完成。
     */
    private var searchJob: Job? = null

    init { loadRecentSearches() }

    /**
     * 从 [PreferencesManager] 收集最近搜索的响应式流，
     * 并在其变化时更新 [SearchUiState.recentSearches]。
     */
    private fun loadRecentSearches() {
        viewModelScope.launch {
            prefsManager.recentSearchesFlow.collect { searches ->
                _uiState.update { it.copy(recentSearches = searches) }
            }
        }
    }

    /**
     * 搜索字段中每次文本变更时调用。
     *
     * 立即更新 [SearchUiState.query] 并取消任何进行中的搜索。
     * 如果查询为空，则清除商品列表。否则启动一个新协程，
     * 在执行搜索前延迟 300 毫秒（防抖）。
     * 搜索成功后，该查询也会保存为最近搜索。
     *
     * @param query 搜索字段的新文本内容。
     */
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        // 取消之前的搜索任务，确保只有最新的查询被执行。
        searchJob?.cancel()
        if (query.isBlank()) {
            // 查询为空时立即清除结果。
            _uiState.update { it.copy(products = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            // 防抖：在发出 API 调用前等待 300 毫秒。
            delay(300)
            _uiState.update { it.copy(isSearching = true) }
            when (val result = productRepository.searchProducts(query)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSearching = false, products = result.data) }
                    // 将成功的查询持久化到最近搜索。
                    prefsManager.addRecentSearch(query)
                }
                is Resource.Error -> _uiState.update { it.copy(isSearching = false) }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 清除搜索查询和商品列表，并取消任何进行中的搜索。
     * 最近搜索列表会保留，以便用户可以点击历史记录。
     */
    fun clearQuery() {
        _uiState.update { it.copy(query = "", products = emptyList()) }
        searchJob?.cancel()
    }
}
