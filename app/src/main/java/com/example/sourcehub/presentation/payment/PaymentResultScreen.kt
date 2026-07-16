package com.example.sourcehub.presentation.payment

/**
 * 支付结果确认页面。
 *
 * 根据支付结果显示大型成功/错误图标和消息。
 * 成功时显示截断的订单ID和金额，并提供查看订单或返回首页的按钮。
 * 失败时仅显示"返回首页"按钮。
 */
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 支付结果页面。
 *
 * 居中布局，包含图标（勾选圆圈或错误）、标题消息、
 * 成功时可选的订单详情以及上下文操作按钮。
 *
 * @param orderId      订单ID（用于在成功时加载完整详情）
 * @param success      支付成功时为 true；失败/取消时为 false
 * @param onViewOrder  导航到订单详情页面的回调
 * @param onBackHome   返回首页的回调
 * @param viewModel    提供订单详情的 [PaymentResultViewModel]
 */
@Composable
fun PaymentResultScreen(
    orderId: String,
    success: Boolean,
    onViewOrder: () -> Unit,
    onBackHome: () -> Unit,
    viewModel: PaymentResultViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }

    // 最小化脚手架，无顶部应用栏 — 图标是主要视觉元素
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (success) "支付成功" else "支付失败",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (success) "您已成功购买，可前往订单详情下载文件" else "支付未完成，请重试或选择其他支付方式",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (success && uiState.order != null) {
                Spacer(Modifier.height(12.dp))
                // 显示截断的订单ID（前12个字符）和最终金额以供参考
                Text("订单号: ${uiState.order!!.id.take(12)}...")
                Text("金额: ¥${String.format("%.2f", uiState.order!!.finalAmount)}")
            }
            Spacer(Modifier.height(32.dp))
            if (success) {
                Button(onClick = onViewOrder, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("查看订单")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onBackHome, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("返回首页")
                }
            } else {
                Button(onClick = onBackHome, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("返回首页")
                }
            }
        }
    }
}
