package com.example.sourcehub.presentation.orders.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcehub.domain.model.OrderStatus
import com.example.sourcehub.presentation.common.components.EmptyView
import com.example.sourcehub.presentation.common.components.ErrorView
import com.example.sourcehub.presentation.common.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    onOrderClick: (String) -> Unit,
    viewModel: OrderListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("我的订单") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = uiState.selectedStatus == null, onClick = { viewModel.filterByStatus(null) }, label = { Text("全部") })
                }
                items(OrderStatus.entries) { status ->
                    FilterChip(selected = uiState.selectedStatus == status, onClick = { viewModel.filterByStatus(status) }, label = { Text(status.label) })
                }
            }
            Spacer(Modifier.height(8.dp))

            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorView(uiState.error!!, viewModel::loadOrders)
                uiState.filteredOrders.isEmpty() -> EmptyView("暂无订单")
                else -> {
                    LazyColumn {
                        items(uiState.filteredOrders) { order ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onOrderClick(order.id) }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row { Text("订单号: ${order.id.take(12)}...", style = MaterialTheme.typography.bodySmall); Spacer(Modifier.weight(1f)); Text(order.status.label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                                    Spacer(Modifier.height(8.dp))
                                    order.items.forEach { item ->
                                        Text("${item.productTitle} x${item.quantity}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row { Text("${order.createdAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.weight(1f)); Text("¥${String.format("%.2f", order.finalAmount)}", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
