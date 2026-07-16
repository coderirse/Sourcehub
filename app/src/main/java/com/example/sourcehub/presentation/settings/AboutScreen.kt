/**
 * 关于页面，展示应用名称、标语、版本信息，
 * 以及隐私政策和用户协议链接。
 */
package com.example.sourcehub.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 关于页面组件。
 *
 * @param onNavigateBack 用户点击返回箭头时的回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        // 居中布局，包含应用品牌信息和法律链接。
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 应用名称，使用主题色。
            Text("SourceHub", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            // 标语。
            Text("优质资料，触手可及", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            // 版本号和构建号。
            Text("版本 1.0.0", style = MaterialTheme.typography.bodyMedium)
            Text("Build 1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            // 法律链接 — 当前为占位，点击处理为空操作。
            TextButton(onClick = { /* 打开隐私政策 */ }) { Text("隐私政策") }
            TextButton(onClick = { /* 打开用户协议 */ }) { Text("用户协议") }
            Spacer(Modifier.height(16.dp))
            // 版权声明。
            Text("© 2024 SourceHub. All rights reserved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
