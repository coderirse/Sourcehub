package com.example.sourcehub.presentation.payment

/**
 * 支付页面组件。
 *
 * 渲染支付金额、可选支付方式列表（单选按钮卡片）、
 * 处理中状态和"立即支付"按钮。
 * 启用 Android 的 FLAG_SECURE 以防止在此页面上截屏/录屏。
 */
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcehub.domain.model.PaymentMethod
import com.example.sourcehub.presentation.common.components.LoadingIndicator

/**
 * 支付页面。
 *
 * 突出显示订单的最终金额，将 [PaymentMethod] 条目列为可选卡片，
 * 并显示处理中指示器或"立即支付"按钮。
 * 此页面可见时应用截屏保护（FLAG_SECURE）。
 *
 * @param orderId           要加载和支付的订单ID
 * @param onPaymentResult   回调，true 表示成功，false 表示失败/取消
 * @param viewModel         提供界面状态和操作的 [PaymentViewModel]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    orderId: String,
    onPaymentResult: (Boolean) -> Unit,
    viewModel: PaymentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current

    // 在支付页面上防止截屏和录屏
    DisposableEffect(Unit) {
        view.setWindowSecureFlag(true)
        onDispose { view.setWindowSecureFlag(false) }
    }

    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }
    LaunchedEffect(uiState.paymentResult) {
        // 将支付结果转发给父级（成功 → true，其他 → false）
        when (uiState.paymentResult) {
            is com.example.sourcehub.domain.model.PaymentResult.Success -> onPaymentResult(true)
            is com.example.sourcehub.domain.model.PaymentResult.Failure -> onPaymentResult(false)
            is com.example.sourcehub.domain.model.PaymentResult.Cancelled -> onPaymentResult(false)
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("支付") },
                navigationIcon = { IconButton(onClick = { onPaymentResult(false) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        // 获取订单详情时显示全屏加载指示器
        if (uiState.isLoading) { LoadingIndicator(Modifier.padding(padding)); return@Scaffold }

        val order = uiState.order
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("支付金额", style = MaterialTheme.typography.bodyLarge)
            Text(
                "¥${String.format("%.2f", order?.finalAmount ?: 0.0)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(32.dp))

            Text("选择支付方式", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            PaymentMethod.entries.forEach { method ->
                // 使用主容器颜色高亮显示已选支付方式的卡片
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectMethod(method) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.selectedMethod == method) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiState.selectedMethod == method, onClick = { viewModel.selectMethod(method) })
                        Spacer(Modifier.width(12.dp))
                        Text(method.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            if (uiState.isProcessing) {
                // 支付进行中时禁用按钮并显示加载旋转指示器
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("正在处理支付...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Button(
                    onClick = viewModel::processPayment,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("立即支付 ¥${String.format("%.2f", order?.finalAmount ?: 0.0)}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/**
 * 在窗口上切换 FLAG_SECURE 的扩展函数，
 * 当 [enable] 为 true 时防止截屏和录屏，
 * 为 false 时恢复正常行为。
 */
private fun android.view.View.setWindowSecureFlag(enable: Boolean) {
    if (enable) {
        (context as? android.app.Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        (context as? android.app.Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
