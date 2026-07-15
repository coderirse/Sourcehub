package com.example.sourcehub.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSecuritySettings: () -> Unit,
    onAbout: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Download settings
            ListItem(
                headlineContent = { Text("仅WiFi下载") },
                trailingContent = {
                    Switch(checked = uiState.wifiOnly, onCheckedChange = viewModel::toggleWifiOnly)
                },
                leadingContent = { Icon(Icons.Default.Wifi, null) }
            )
            ListItem(
                headlineContent = { Text("生物识别锁定") },
                supportingContent = { Text("使用指纹或面部识别保护敏感文件") },
                trailingContent = {
                    Switch(checked = uiState.biometricLock, onCheckedChange = viewModel::toggleBiometricLock)
                },
                leadingContent = { Icon(Icons.Default.Fingerprint, null) }
            )
            ListItem(
                headlineContent = { Text("使用远程服务器") },
                supportingContent = { Text("连接 Ktor 后端 API（需先启动 server/）") },
                trailingContent = {
                    Switch(checked = uiState.useRemoteApi, onCheckedChange = viewModel::toggleRemoteApi)
                },
                leadingContent = { Icon(Icons.Default.Cloud, null) }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent = { Text("安全设置") },
                leadingContent = { Icon(Icons.Default.Security, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable(onClick = onSecuritySettings)
            )
            ListItem(
                headlineContent = { Text("清除缓存") },
                supportingContent = { Text("当前缓存: ${uiState.cacheSize}") },
                leadingContent = { Icon(Icons.Default.DeleteSweep, null) },
                modifier = Modifier.clickable(onClick = viewModel::clearCache)
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent = { Text("关于") },
                leadingContent = { Icon(Icons.Default.Info, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable(onClick = onAbout)
            )
            ListItem(
                headlineContent = { Text("版本") },
                supportingContent = { Text("1.0.0") },
                leadingContent = { Icon(Icons.Default.Info, null) }
            )
        }
    }
}
