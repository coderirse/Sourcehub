package com.example.sourcehub.presentation.common.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/**
 * 预配置的确认对话框，封装 Material 3 [AlertDialog]。
 *
 * 显示 [title] 和 [message]，包含两个操作按钮：
 * - **确认**（红色，使用 [MaterialTheme.colorScheme.error]）——调用 [onConfirm]。
 * - **取消**（默认样式）——调用 [onDismiss]。
 *
 * 两个按钮标签可通过 [confirmText] 和 [dismissText] 自定义，
 * 默认分别为"确定"和"取消"。对话框可通过外部点击或返回键关闭，
 * 触发 [onDismissRequest]（路由到 [onDismiss]）。
 *
 * 此组件通常根据宿主页面状态中的布尔标志条件性地显示，例如：
 * ```
 * if (showDialog) {
 *     ConfirmDialog(
 *         title = "删除确认",
 *         message = "确定要删除这个项目吗？此操作不可撤销。",
 *         onConfirm = { viewModel.delete(); showDialog = false },
 *         onDismiss = { showDialog = false }
 *     )
 * }
 * ```
 *
 * @param title       显示在头部的对话框标题。
 * @param message     解释操作的正文文本。
 * @param confirmText 确认按钮的标签（默认："确定"）。
 * @param dismissText 取消按钮的标签（默认："取消"）。
 * @param onConfirm   点击确认按钮时调用的回调。
 * @param onDismiss   点击取消按钮或对话框被外部关闭时调用的回调。
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                // 确认操作以错误色样式显示，作为警告提示
                Text(confirmText, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
