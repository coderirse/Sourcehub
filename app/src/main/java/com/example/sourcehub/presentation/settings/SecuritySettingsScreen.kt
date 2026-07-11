package com.example.sourcehub.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            ListItem(
                headlineContent = { Text("修改密码") },
                leadingContent = { Icon(Icons.Default.Lock, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) }
            )
            ListItem(
                headlineContent = { Text("生物识别锁定") },
                supportingContent = { Text("使用指纹或面部识别解锁敏感文件") },
                leadingContent = { Icon(Icons.Default.Fingerprint, null) },
                trailingContent = { Switch(checked = false, onCheckedChange = {}) }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            val securityFlags = com.example.sourcehub.SourcehubApplication.securityFlags
            ListItem(
                headlineContent = { Text("设备安全状态") },
                supportingContent = {
                    Text("Root: ${if (securityFlags.isRooted) "已检测" else "安全"}\n模拟器: ${if (securityFlags.isEmulator) "已检测" else "安全"}\n调试器: ${if (securityFlags.isDebugged) "已检测" else "安全"}")
                },
                leadingContent = { Icon(if (securityFlags.isRooted || securityFlags.isEmulator) Icons.Default.Warning else Icons.Default.CheckCircle, null, tint = if (securityFlags.isRooted || securityFlags.isEmulator) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
            )
            ListItem(
                headlineContent = { Text("登录历史") },
                supportingContent = { Text("最近登录: 2024-01-15 10:30") },
                leadingContent = { Icon(Icons.Default.History, null) }
            )
        }
    }
}
