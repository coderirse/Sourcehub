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

/**
 * 订单列表页面。
 *
 * 展示用户的所有订单，支持按订单状态（全部 / 待支付 / 已支付等）进行筛选过滤。
 * 每个订单卡片显示订单号（截断前12位）、状态标签、商品摘要、下单时间和实付金额。
 * 点击卡片导航至 [onOrderClick] 指定的订单详情页面。
 *
 * 页面状态由 [OrderListViewModel] 驱动，通过 [OrderListUiState] 暴露加载、错误和筛选后的订单数据。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    /** 点击某个订单卡片时的回调，传入订单 ID。 */
    onOrderClick: (String) -> Unit,
    /** 订单列表的 ViewModel，默认通过 [viewModel] 获取。 */
    viewModel: OrderListViewModel = viewModel()
) {
    // 收集 UI 状态，使用生命周期感知的 Flow 收集
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("我的订单") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 横向滚动的状态筛选栏："全部" 按钮 + 每个 OrderStatus 枚举值
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    // selectedStatus == null 表示未筛选，即"全部"
                    FilterChip(selected = uiState.selectedStatus == null, onClick = { viewModel.filterByStatus(null) }, label = { Text("全部") })
                }
                items(OrderStatus.entries) { status ->
                    FilterChip(selected = uiState.selectedStatus == status, onClick = { viewModel.filterByStatus(status) }, label = { Text(status.label) })
                }
            }
            Spacer(Modifier.height(8.dp))

            // 根据当前状态展示不同 UI：加载中 → 错误 → 空列表 → 订单卡片列表
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
                                    // 首行：订单号（截断显示前12位）+ 状态标签
                                    Row { Text("订单号: ${order.id.take(12)}...", style = MaterialTheme.typography.bodySmall); Spacer(Modifier.weight(1f)); Text(order.status.label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                                    Spacer(Modifier.height(8.dp))
                                    // 商品摘要：每个商品标题和数量
                                    order.items.forEach { item ->
                                        Text("${item.productTitle} x${item.quantity}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    // 末行：下单时间 + 实付金额（右对齐）
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
