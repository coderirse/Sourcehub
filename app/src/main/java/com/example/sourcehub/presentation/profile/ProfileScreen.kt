package com.example.sourcehub.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.sourcehub.presentation.common.components.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onOrders: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        ConfirmDialog(
            title = "确认退出",
            message = "确定要退出登录吗？",
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("我的") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Profile header
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp).clickable(onClick = onEditProfile)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = uiState.user?.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(uiState.user?.name ?: "未登录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(uiState.user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Menu items
            ProfileMenuItem(Icons.Default.Receipt, "我的订单", onClick = onOrders)
            ProfileMenuItem(Icons.Default.Download, "下载管理", onClick = onDownloads)
            ProfileMenuItem(Icons.Default.FolderOpen, "离线文件", onClick = onDownloads)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            ProfileMenuItem(Icons.Default.Settings, "设置", onClick = onSettings)
            ProfileMenuItem(Icons.AutoMirrored.Filled.Logout, "退出登录", onClick = { showLogoutDialog = true }, isDestructive = true)
        }
    }
}

@Composable
private fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit, isDestructive: Boolean = false) {
    ListItem(
        headlineContent = { Text(title, color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
        leadingContent = { Icon(icon, null, tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
