/**
 * 按分类筛选并以双列网格展示商品的页面。
 *
 * 当 [categoryId] 为空时，该页面展示所有新品商品（最多 50 个）。
 * 顶部应用栏提供返回箭头和分类名称（或回退标签"全部商品"）的导航功能。
 */
package com.example.sourcehub.presentation.product.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcehub.presentation.common.components.ErrorView
import com.example.sourcehub.presentation.common.components.LoadingIndicator
import com.example.sourcehub.presentation.common.components.ProductCard

/**
 * 按分类筛选的商品列表页面。
 *
 * 使用以 [categoryId] 为键的 [LaunchedEffect]，当所选分类变化时重新加载商品。
 *
 * @param categoryId 筛选所用的分类 ID。空字符串表示"全部商品"。
 * @param categoryName 显示在顶部应用栏中的可读分类名称。
 * @param onProductClick 用户点击商品卡片时调用，参数为商品 ID。
 * @param onNavigateBack 用户按下返回箭头时调用。
 * @param viewModel 驱动此页面的 [ProductListViewModel]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    categoryId: String,
    categoryName: String,
    onProductClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProductListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 每当分类变化时重新加载商品。
    LaunchedEffect(categoryId) { viewModel.loadProducts(categoryId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName.ifBlank { "全部商品" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(padding))
            uiState.error != null -> ErrorView(uiState.error!!, viewModel::retry, Modifier.padding(padding))
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
