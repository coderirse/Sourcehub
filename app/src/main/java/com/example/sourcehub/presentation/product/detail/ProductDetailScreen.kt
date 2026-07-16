/**
 * 商品详情页面，展示完整的商品信息、封面图片、元数据、描述、标签
 * 以及操作按钮（加入购物车、立即购买、预览）。
 *
 * 该页面由 [ProductDetailViewModel] 驱动，它从仓库加载商品详情
 * 并处理购物车和订单操作。
 */
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

/**
 * 完整的商品详情页面，包含封面图片、元数据、描述、标签
 * 以及固定在底部的操作栏。
 *
 * 以 [productId] 为键的 [LaunchedEffect] 会在商品变更时触发重新加载。
 * 第二个 [LaunchedEffect] 监视 [ProductDetailUiState.cartAddedMessage]，
 * 并在 2 秒后自动关闭 snackbar。
 *
 * 底部操作栏在可滚动内容之外的 [Surface] 中渲染，
 * 因此当用户滚动上方商品详情时它保持固定。
 *
 * @param productId 要展示的商品 ID。
 * @param onNavigateBack 按下返回箭头时调用。
 * @param onPreview 点击"预览"按钮时调用。
 * @param onBuyNow 成功创建立即购买订单后调用，参数为订单 ID。
 * @param onAddToCartSuccess 商品加入购物车后调用（用于分析/导航）。
 * @param viewModel 驱动此页面的 [ProductDetailViewModel]。
 */
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

    // 每当商品 ID 变化时加载商品详情。
    LaunchedEffect(productId) { viewModel.loadProduct(productId) }

    // 2 秒后自动关闭"已加入购物车"snackbar 消息。
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
            // 当购物车确认消息非空时显示 snackbar。
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
                    // 可滚动内容区域占据底部操作栏上方的全部剩余空间。
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        // 封面图片，使用纵向宽高比（0.7 = 宽 / 高）。
                        AsyncImage(
                            model = product.coverUrl,
                            contentDescription = product.title,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(product.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            // 价格行：当前价格使用主色调；原价在打折时以划线样式展示。
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
                            // 文件信息：类型扩展名、页数以及从字节转换为 MB 的文件大小。
                            Text("文件类型: ${product.fileType.extension.uppercase()} | ${product.pageCount}页 | ${product.fileSize / 1024 / 1024}MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = onPreview, modifier = Modifier.fillMaxWidth()) { Text("预览") }
                            Spacer(Modifier.height(16.dp))
                            Text("商品描述", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(product.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                            // 标签以中间点分隔符连接。
                            Text("标签: ${product.tags.joinToString(" · ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // 底部操作栏 — 固定在可滚动内容下方，带阴影以实现视觉分隔。
                    Surface(shadowElevation = 8.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = viewModel::addToCart,
                                // 加入购物车请求进行中时禁用按钮。
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
