package com.example.sourcehub.presentation.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 全屏错误占位组件，带有可选的重试按钮。
 *
 * 显示一个以主题错误色着色的错误轮廓图标、一条描述性的 [message]，
 * 并且当 [onRetry] 非空时，显示一个调用该回调的"重试"[Button]。
 *
 * 当 [Resource] 或
 * [com.example.sourcehub.presentation.common.state.UiState] 持有失败状态时，
 * 通常以此组件作为"错误"分支展示。
 *
 * 用法：
 * ```
 * ErrorView(
 *     message = "网络连接失败",
 *     onRetry = { viewModel.refresh() }
 * )
 * ```
 *
 * @param message  向用户展示的人类可读错误描述。
 * @param onRetry  点击重试按钮时调用的可选回调。
 *     为 `null` 时按钮完全省略。
 * @param modifier  外部容器的可选 [Modifier]。
 */
@Composable
fun ErrorView(
    message: String,
    onRetry: (() -> Unit)? = null,
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
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null, // 装饰性图标——无需内容描述
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            // 仅当提供了处理函数时才显示重试按钮
            if (onRetry != null) {
                Button(onClick = onRetry) {
                    Text("重试")
                }
            }
        }
    }
}
