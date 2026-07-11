package com.example.sourcehub.presentation.payment

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
