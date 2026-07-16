package com.example.sourcehub.presentation.orders.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcehub.presentation.common.components.ErrorView
import com.example.sourcehub.presentation.common.components.LoadingIndicator

/**
 * 订单详情页面。
 *
 * 展示单个订单的完整信息，包括订单号、状态、支付方式、优惠码（若有）、
 * 商品列表（含单价和数量）以及金额明细（小计、优惠、实付）。
 * 对于状态为 [OrderStatus.PAID] 的订单，每个商品行右侧显示下载按钮，
 * 点击后通过 [OrderDetailViewModel.startDownload] 发起下载并回调 [onDownload]。
 *
 * 页面状态由 [OrderDetailViewModel] 驱动，根据 [OrderDetailUiState] 展示加载中、
 * 错误或订单详情三种 UI。
 *
 * @param orderId 要展示的订单 ID。
 * @param onNavigateBack 返回上一页的回调。
 * @param onDownload 点击下载按钮时的回调，传入商品 ID。
 * @param viewModel 订单详情的 ViewModel。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    onNavigateBack: () -> Unit,
    onDownload: (String) -> Unit,
    viewModel: OrderDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 当 orderId 变化时重新加载订单数据
    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("订单详情") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        // 根据 UI 状态展示对应内容
        when {
            uiState.isLoading -> LoadingIndicator(Modifier.padding(padding))
            uiState.error != null -> ErrorView(uiState.error!!, { viewModel.loadOrder(orderId) }, Modifier.padding(padding))
            uiState.order != null -> {
                val order = uiState.order!!
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    // 订单基本信息区域
                    Text("订单号", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(order.id, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Row { Text("状态: ", style = MaterialTheme.typography.bodyMedium); Text(order.status.label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                    Spacer(Modifier.height(12.dp))
                    Text("支付方式: ${order.paymentMethod.label}")
                    // 仅当使用了优惠码时才显示
                    if (order.couponCode.isNotEmpty()) { Text("优惠码: ${order.couponCode}") }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // 商品列表区域
                    Text("商品列表", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    order.items.forEach { item ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.productTitle, style = MaterialTheme.typography.bodyMedium)
                                    Text("¥${String.format("%.2f", item.unitPrice)} x ${item.quantity}", style = MaterialTheme.typography.bodySmall)
                                }
                                // 仅已支付订单才显示下载按钮，用户购买后可下载对应商品
                                if (order.status == com.example.sourcehub.domain.model.OrderStatus.PAID) {
                                    IconButton(onClick = { viewModel.startDownload(item.productId); onDownload(item.productId) }) {
                                        Icon(Icons.Default.Download, "下载")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // 金额明细区域
                    Row { Text("小计", Modifier.weight(1f)); Text("¥${String.format("%.2f", order.totalAmount)}") }
                    // 仅当有优惠时才显示优惠行
                    if (order.discountAmount > 0) Row { Text("优惠", Modifier.weight(1f)); Text("-¥${String.format("%.2f", order.discountAmount)}") }
                    Row { Text("实付", Modifier.weight(1f), fontWeight = FontWeight.Bold); Text("¥${String.format("%.2f", order.finalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}
