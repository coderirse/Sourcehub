package com.example.sourcehub.presentation.cart

/**
 * 购物车页面组件。
 *
 * 显示用户的购物车，包含可选择的商品、数量控件和结算按钮。
 * 支持全选、单独切换和动态总计计算。
 * 当购物车为空时显示空状态占位符。
 */
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.example.sourcehub.presentation.common.components.EmptyView

/**
 * 购物车页面。
 *
 * 渲染一个标题为"购物车"的顶部应用栏，一个包含总计摘要的全选行，
 * 一个包含复选框/图片/标题/价格/数量控件/删除按钮的购物车商品卡片延迟列表，
 * 以及一个仅当至少选中一个商品时才启用的底部固定结算按钮。
 *
 * @param onCheckout  用户点击结算按钮时调用的回调
 * @param onProductClick  用户点击商品封面图片时调用的回调，接收商品ID
 * @param viewModel  提供界面状态和操作的 [CartViewModel]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onCheckout: () -> Unit,
    onProductClick: (String) -> Unit,
    viewModel: CartViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("购物车") }) }
    ) { padding ->
        if (uiState.items.isEmpty()) {
            // 空状态，显示一个引导用户浏览商品的操作按钮
            EmptyView("购物车是空的", "去逛逛", onAction = { /* 导航到首页 */ }, modifier = Modifier.padding(padding))
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 全选复选框：仅当所有商品都被选中时勾选
                    Checkbox(
                        checked = uiState.selectedIds.size == uiState.items.size,
                        onCheckedChange = { viewModel.toggleSelectAll() }
                    )
                    Text("全选", modifier = Modifier.weight(1f))
                    Text("合计: ¥${String.format("%.2f", uiState.totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.items, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.id in uiState.selectedIds,
                                    onCheckedChange = { viewModel.toggleSelect(item.id) }
                                )
                                AsyncImage(
                                    model = item.productCover,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp).clickable { onProductClick(item.productId) },
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                    Text(item.productTitle, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                                    Text("¥${String.format("%.2f", item.price)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { viewModel.updateQuantity(item.id, item.quantity - 1) }) { Text("-") }
                                        Text("${item.quantity}")
                                        IconButton(onClick = { viewModel.updateQuantity(item.id, item.quantity + 1) }) { Text("+") }
                                    }
                                }
                                IconButton(onClick = { viewModel.removeItem(item.id) }) {
                                    Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onCheckout,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                    enabled = uiState.selectedIds.isNotEmpty()
                ) {
                    Text("结算 (${uiState.selectedIds.size})")
                }
            }
        }
    }
}
