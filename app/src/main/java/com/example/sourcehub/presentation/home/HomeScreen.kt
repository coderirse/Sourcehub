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
                    IconButton(onClick = { /* search navigation handled by bottom nav */ }) {
                        Icon(Icons.Default.Search, "搜索")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.recommendedProducts.isEmpty()) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else if (uiState.error != null && uiState.recommendedProducts.isEmpty()) {
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
                // Banner Carousel
                BannerCarousel(
                    banners = uiState.banners,
                    onBannerClick = onBannerClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Categories
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

                // Recommended Products
                SectionHeader(title = "热门推荐")
                Spacer(modifier = Modifier.height(8.dp))
                ProductGrid(
                    products = uiState.recommendedProducts,
                    onProductClick = onProductClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                // New Arrivals
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

@Composable
private fun CategoryChip(category: Category, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(category.name) }
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun ProductGrid(products: List<com.example.sourcehub.domain.model.Product>, onProductClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(600.dp),
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
