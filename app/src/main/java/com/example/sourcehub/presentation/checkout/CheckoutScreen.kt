package com.example.sourcehub.presentation.checkout

/**
 * 订单结算/确认页面。
 *
 * 显示购物车商品列表，允许用户使用优惠码，展示小计/优惠/总计明细，
 * 并通过 [CheckoutViewModel] 提交订单。
 * 当订单成功创建后，组件会自动导航到支付流程。
 */
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.CartItem

/**
 * 订单确认页面。
 *
 * 显示购物车中的每件商品及其数量和行总计，优惠码输入框，
 * 价格摘要（小计、优惠、总计）和提交按钮。
 * 成功后，页面将新创建的订单ID发送给 [onPayment] 回调。
 *
 * @param onNavigateBack  导航到上一页面的回调
 * @param onPayment       接收创建的订单ID以继续支付的回调
 * @param viewModel       提供界面状态和操作的 [CheckoutViewModel]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onNavigateBack: () -> Unit,
    onPayment: (String) -> Unit,
    viewModel: CheckoutViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appContainer = SourcehubApplication.instance.appContainer
    val cartItems by appContainer.cartRepository.getCartItems(appContainer.authRepository.getUserId())
        .collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(uiState.createdOrderId) {
        // 订单创建成功后立即导航到支付页面
        uiState.createdOrderId?.let { onPayment(it) }
    }

    // 根据实时购物车商品计算小计、优惠和最终总计
    val subtotal = cartItems.sumOf { it.price * it.quantity }
    val discount = if (uiState.discountAmount > 0) subtotal * uiState.discountAmount else 0.0
    val total = subtotal - discount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("确认订单") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            // 保护：当用户购物车为空时进入结算页面，显示提示
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("购物车为空，请先添加商品")
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            ) {
                Text("订单商品", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                cartItems.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(item.productTitle, modifier = Modifier.weight(1f))
                        Text("x${item.quantity}")
                        Text(" ¥${String.format("%.2f", item.price * item.quantity)}")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.couponCode,
                    onValueChange = viewModel::onCouponChange,
                    label = { Text("优惠码") },
                    trailingIcon = { TextButton(onClick = viewModel::applyCoupon) { Text("使用") } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    singleLine = true
                )
                // 无效优惠码的内联错误提示横幅
                if (uiState.error != null) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(12.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row { Text("小计", modifier = Modifier.weight(1f)); Text("¥${String.format("%.2f", subtotal)}") }
                    if (discount > 0) {
                        // 仅在实际应用优惠时才显示优惠行
                        Row { Text("优惠", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error); Text("-¥${String.format("%.2f", discount)}", color = MaterialTheme.colorScheme.error) }
                    }
                    HorizontalDivider()
                    Row {
                        Text("应付总额", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("¥${String.format("%.2f", total)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.placeOrder(cartItems) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
                    enabled = !uiState.isLoading
                ) {
                    // 提交订单时在按钮内显示加载旋转指示器
                    if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("提交订单 ¥${String.format("%.2f", total)}")
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
