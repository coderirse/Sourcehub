/**
 * 设置页面，提供应用级配置开关：
 * 仅WiFi下载、生物识别锁定、远程API使用，
 * 并提供导航至安全设置和关于页面。
 */
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

/**
 * 设置页面组件。
 *
 * @param onNavigateBack 用户点击返回箭头时的回调。
 * @param onSecuritySettings 用户点击"安全设置"时的回调。
 * @param onAbout 用户点击"关于"时的回调。
 * @param viewModel 驱动开关状态和操作的 [SettingsViewModel]。默认为作用域内的 ViewModel。
 */
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
            // ---- 下载与安全开关 ----

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
            // 切换本地与远程（Ktor）API 后端的开关。
            ListItem(
                headlineContent = { Text("使用远程服务器") },
                supportingContent = { Text("连接 Ktor 后端 API（需先启动 server/）") },
                trailingContent = {
                    Switch(checked = uiState.useRemoteApi, onCheckedChange = viewModel::toggleRemoteApi)
                },
                leadingContent = { Icon(Icons.Default.Cloud, null) }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            // ---- 导航条目 ----

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

            // ---- 信息条目 ----

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
