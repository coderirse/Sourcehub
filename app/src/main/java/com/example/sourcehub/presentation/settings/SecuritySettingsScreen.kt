/**
 * 安全设置页面，展示设备安全状态、
 * 生物识别锁定开关和登录历史。
 *
 * 直接读取 [com.example.sourcehub.SourcehubApplication.securityFlags]
 * 以显示 root、模拟器和调试器检测结果。
 */
package com.example.sourcehub.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 安全设置页面组件。
 *
 * @param onNavigateBack 用户点击返回箭头时的回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("安全设置") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 修改密码的占位条目（尚未接入功能）。
            ListItem(
                headlineContent = { Text("修改密码") },
                leadingContent = { Icon(Icons.Default.Lock, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) }
            )
            // 生物识别锁定开关 — 当前未接入功能（始终为 false）。
            ListItem(
                headlineContent = { Text("生物识别锁定") },
                supportingContent = { Text("使用指纹或面部识别解锁敏感文件") },
                leadingContent = { Icon(Icons.Default.Fingerprint, null) },
                trailingContent = { Switch(checked = false, onCheckedChange = {}) }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            // 读取全局安全标记以显示设备完整性状态。
            val securityFlags = com.example.sourcehub.SourcehubApplication.securityFlags
            ListItem(
                headlineContent = { Text("设备安全状态") },
                supportingContent = {
                    Text("Root: ${if (securityFlags.isRooted) "已检测" else "安全"}\n模拟器: ${if (securityFlags.isEmulator) "已检测" else "安全"}\n调试器: ${if (securityFlags.isDebugged) "已检测" else "安全"}")
                },
                // 若检测到任何安全问题则显示警告图标；否则显示对勾圆圈。
                leadingContent = { Icon(if (securityFlags.isRooted || securityFlags.isEmulator) Icons.Default.Warning else Icons.Default.CheckCircle, null, tint = if (securityFlags.isRooted || securityFlags.isEmulator) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
            )
            // 静态的登录历史占位信息。
            ListItem(
                headlineContent = { Text("登录历史") },
                supportingContent = { Text("最近登录: 2024-01-15 10:30") },
                leadingContent = { Icon(Icons.Default.History, null) }
            )
        }
    }
}
