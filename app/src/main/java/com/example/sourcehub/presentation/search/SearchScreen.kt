/**
 * 搜索页面，顶部应用栏中有搜索文本框、搜索过程中显示加载指示器、
 * 空结果视图、最近搜索历史（查询为空时显示）以及匹配商品的双列网格。
 *
 * 该页面由 [SearchViewModel] 驱动，它对查询变更进行 300 毫秒防抖处理，
 * 并通过 [PreferencesManager] 持久化最近搜索记录。
 */
package com.example.sourcehub.presentation.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcehub.presentation.common.components.EmptyView
import com.example.sourcehub.presentation.common.components.ProductCard

/**
 * 搜索页面组合组件。
 *
 * 主体内容在四种状态之间切换：
 * 1. [SearchUiState.isSearching] — 居中显示的 [CircularProgressIndicator]。
 * 2. 查询非空但无结果 — 显示"未找到相关结果"消息的 [EmptyView]。
 * 3. 查询为空 — 最近搜索历史列表。
 * 4. 有结果 — 双列 [LazyVerticalGrid] 展示 [ProductCard]。
 *
 * @param onProductClick 搜索结果的商品被点击时调用，参数为商品 ID。
 * @param viewModel 驱动此页面的 [SearchViewModel]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onProductClick: (String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("搜索资料...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            // 仅在查询非空时显示清除（X）图标。
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = viewModel::clearQuery) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    ) { padding ->
        when {
            // 搜索中 — 防抖查询进行中时显示居中的旋转指示器。
            uiState.isSearching -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            // 已输入查询但未找到结果。
            uiState.products.isEmpty() && uiState.query.isNotEmpty() -> {
                EmptyView("未找到相关结果", modifier = Modifier.padding(padding))
            }
            // 查询为空 — 以可点击文本按钮展示最近搜索历史。
            uiState.products.isEmpty() -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text("最近搜索", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    uiState.recentSearches.forEach { query ->
                        TextButton(onClick = { viewModel.onQueryChange(query) }) { Text(query) }
                    }
                }
            }
            // 有结果 — 双列商品网格。
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(padding)
                ) {
                    items(uiState.products) { product ->
                        ProductCard(product = product, onClick = { onProductClick(product.id) })
                    }
                }
            }
        }
    }
}
