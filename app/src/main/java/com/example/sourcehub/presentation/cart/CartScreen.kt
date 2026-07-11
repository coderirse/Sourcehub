package com.example.sourcehub.presentation.cart

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
            EmptyView("购物车是空的", "去逛逛", onAction = { /* navigate to home */ }, modifier = Modifier.padding(padding))
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
