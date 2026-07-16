/**
 * SourceHub 应用的主首页。
 *
 * 显示一个垂直可滚动的信息流，包含横幅轮播、分类标签、
 * "热门推荐"网格和"最新上架"网格。每个区域由 [HomeViewModel]
 * 支持，它在初始化时从商品仓库获取数据并通过单一的 [HomeUiState] 流暴露。
 *
 * 导航事件（商品点击、分类点击、横幅点击）通过回调 lambda
 * 提升给调用方，使此组合组件保持为纯界面层。
 */
package com.example.sourcehub.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcehub.domain.model.Banner
import com.example.sourcehub.domain.model.Category
import com.example.sourcehub.presentation.common.components.BannerCarousel
import com.example.sourcehub.presentation.common.components.ErrorView
import com.example.sourcehub.presentation.common.components.LoadingIndicator
import com.example.sourcehub.presentation.common.components.ProductCard

/**
 * 首页标签的根组合组件。
 *
 * 渲染带有品牌顶部栏的 [Scaffold]。主体在以下状态之间切换：
 * - 首次加载数据时显示全屏 [LoadingIndicator]。
 * - 加载失败且无缓存数据时显示 [ErrorView]。
 * - 包含横幅轮播、分类标签、推荐商品和最新上架商品的可滚动列。
 *
 * @param onProductClick 商品卡片被点击时以商品 ID 调用。
 * @param onCategoryClick 分类标签被点击时以分类 ID 和名称调用。
 * @param onBannerClick 横幅被点击时以 [Banner] 模型调用。
 * @param viewModel 驱动此页面的 [HomeViewModel]。默认为 Compose 运行时
 *   提供的作用域视图模型。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProductClick: (String) -> Unit,
    onCategoryClick: (String, String) -> Unit,
    onBannerClick: (Banner) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SourceHub", fontWeight = FontWeight.Bold) },
                actions = {
                    // 搜索图标存在，但搜索导航由底部导航栏处理。
                    IconButton(onClick = { /* 搜索导航由底部导航栏处理 */ }) {
                        Icon(Icons.Default.Search, "搜索")
                    }
                }
            )
        }
    ) { padding ->
        // 仅在初始加载且无缓存商品时显示全屏加载指示器。
        if (uiState.isLoading && uiState.recommendedProducts.isEmpty()) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else if (uiState.error != null && uiState.recommendedProducts.isEmpty()) {
            // 仅在无缓存数据可显示时显示带重试的错误视图。
            ErrorView(
                message = uiState.error!!,
                onRetry = viewModel::loadData,
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // 横幅轮播
                BannerCarousel(
                    banners = uiState.banners,
                    onBannerClick = onBannerClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 分类 — 可水平滚动的 FilterChip 行。
                Text(
                    text = "分类",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.categories) { category ->
                        CategoryChip(
                            category = category,
                            onClick = { onCategoryClick(category.id, category.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 推荐商品
                SectionHeader(title = "热门推荐")
                Spacer(modifier = Modifier.height(8.dp))
                ProductGrid(
                    products = uiState.recommendedProducts,
                    onProductClick = onProductClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 最新上架
                SectionHeader(title = "最新上架")
                Spacer(modifier = Modifier.height(8.dp))
                ProductGrid(
                    products = uiState.newArrivals,
                    onProductClick = onProductClick
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 渲染为未选中 [FilterChip] 的单个分类。
 *
 * @param category 要显示的分类模型。
 * @param onClick 标签被点击时调用。
 */
@Composable
private fun CategoryChip(category: Category, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(category.name) }
    )
}

/**
 * 每个商品网格前使用的粗体区域标题。
 *
 * @param title 要显示的标题文本。
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

/**
 * 固定高度的 2 列垂直商品卡片网格。
 *
 * 网格本身的滚动被禁用，因为它嵌套在已垂直滚动的
 * 父 [Column] 内部 — 同时启用两者会导致嵌套滚动冲突。
 *
 * @param products 要显示的商品列表。
 * @param onProductClick 卡片被点击时以商品 ID 调用。
 */
@Composable
private fun ProductGrid(products: List<com.example.sourcehub.domain.model.Product>, onProductClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(600.dp),
        // 嵌套滚动已禁用，避免与外部 Column 滚动冲突。
        userScrollEnabled = false
    ) {
        items(products) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product.id) }
            )
        }
    }
}
