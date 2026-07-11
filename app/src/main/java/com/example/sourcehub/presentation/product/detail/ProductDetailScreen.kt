package com.example.sourcehub.presentation.product.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.sourcehub.presentation.common.components.ErrorView
import com.example.sourcehub.presentation.common.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    onPreview: () -> Unit,
    onBuyNow: (String) -> Unit,
    onAddToCartSuccess: () -> Unit,
    viewModel: ProductDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appContainer = com.example.sourcehub.SourcehubApplication.instance.appContainer

    LaunchedEffect(productId) { viewModel.loadProduct(productId) }
    LaunchedEffect(uiState.cartAddedMessage) {
        if (uiState.cartAddedMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearCartMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("商品详情") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        },
        snackbarHost = {
            if (uiState.cartAddedMessage != null) {
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(uiState.cartAddedMessage!!) }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(Modifier.padding(padding))
            uiState.error != null -> ErrorView(uiState.error!!, { viewModel.loadProduct(productId) }, Modifier.padding(padding))
            uiState.product != null -> {
                val product = uiState.product!!
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        AsyncImage(
                            model = product.coverUrl,
                            contentDescription = product.title,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(product.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("¥${String.format("%.2f", product.price)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (product.originalPrice > product.price) {
                                    Spacer(Modifier.width(8.dp))
                                    Text("¥${String.format("%.2f", product.originalPrice)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row { Text("作者: ${product.author}", style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.width(16.dp)); Text("销量: ${product.salesCount}", style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.width(16.dp)); Text("评分: ${product.rating}", style = MaterialTheme.typography.bodyMedium) }
                            Spacer(Modifier.height(4.dp))
                            Text("文件类型: ${product.fileType.extension.uppercase()} | ${product.pageCount}页 | ${product.fileSize / 1024 / 1024}MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = onPreview, modifier = Modifier.fillMaxWidth()) { Text("预览") }
                            Spacer(Modifier.height(16.dp))
                            Text("商品描述", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(product.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                            Text("标签: ${product.tags.joinToString(" · ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // Bottom bar
                    Surface(shadowElevation = 8.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = viewModel::addToCart,
                                enabled = !uiState.isAddingToCart,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ShoppingCart, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("加入购物车")
                            }
                            Button(
                                onClick = { viewModel.buyNow(onBuyNow) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("立即购买")
                            }
                        }
                    }
                }
            }
        }
    }
}
