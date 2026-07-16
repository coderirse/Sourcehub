package com.example.sourcehub.presentation.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 全屏居中的加载旋转指示器。
 *
 * 此组件填充可用空间，在中央显示一个使用主题主色的
 * [CircularProgressIndicator]。
 * 这是整个应用中最简单的"加载中"分支，用于无需保留额外
 * 装饰（工具栏、底部导航等）可见的场景。
 *
 * 用法：
 * ```
 * LoadingIndicator()
 * // 或使用自定义修饰符：
 * LoadingIndicator(modifier = Modifier.padding(16.dp))
 * ```
 *
 * @param modifier  应用于外部容器的可选 [Modifier]。
 *     如果在此提供自定义修饰符，将替换默认的 `fillMaxSize()`。
 */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
