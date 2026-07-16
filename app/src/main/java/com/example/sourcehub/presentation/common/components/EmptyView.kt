package com.example.sourcehub.presentation.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 全屏空状态占位组件，带有可选的操作按钮。
 *
 * 显示一个降低透明度的收件箱图标、一条描述性的 [message]，
 * 并且当 [actionLabel] 和 [onAction] 均非空时，显示一个调用回调的
 * [OutlinedButton]。此组件用于列表或搜索结果为空，
 * 或用户尚未添加任何数据的场景。
 *
 * 用法：
 * ```
 * EmptyView(
 *     message = "还没有任何产品",
 *     actionLabel = "去逛逛",
 *     onAction = { navController.navigate("shop") }
 * )
 * ```
 *
 * @param message     人类可读的消息，解释页面为空的原因。
 * @param actionLabel 可选的按钮文本。仅当非空且 [onAction] 也非空时才显示。
 * @param onAction    点击操作按钮时调用的可选回调。
 * @param modifier    外部容器的可选 [Modifier]。
 */
@Composable
fun EmptyView(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null, // 装饰性图标——无需内容描述
                modifier = Modifier.size(64.dp),
                // 半透明以在视觉上与错误状态区分
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            // 标签和处理函数必须同时存在才会渲染按钮
            if (actionLabel != null && onAction != null) {
                OutlinedButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}
