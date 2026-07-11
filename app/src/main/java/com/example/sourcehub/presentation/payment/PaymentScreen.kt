package com.example.sourcehub.presentation.payment

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    orderId: String,
    onPaymentResult: (Boolean) -> Unit,
    viewModel: PaymentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current

    // FLAG_SECURE for payment screen
    DisposableEffect(Unit) {
        view.setWindowSecureFlag(true)
        onDispose { view.setWindowSecureFlag(false) }
    }

    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }
    LaunchedEffect(uiState.paymentResult) {
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

private fun android.view.View.setWindowSecureFlag(enable: Boolean) {
    if (enable) {
        (context as? android.app.Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        (context as? android.app.Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
