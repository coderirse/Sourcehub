/**
 * 个人资料页面，展示当前用户的头像、昵称、邮箱，
 * 并提供导航至订单、下载、设置和退出登录的入口。
 *
 * 用户退出登录前会弹出确认对话框。
 */
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

/**
 * 个人资料主页面组件。
 *
 * @param onEditProfile 用户点击个人资料头部卡片以编辑资料时的回调。
 * @param onOrders 用户点击"我的订单"时的回调。
 * @param onDownloads 用户点击"下载管理"或"离线文件"时的回调。
 * @param onSettings 用户点击"设置"时的回调。
 * @param onLogout 用户确认退出登录对话框后的回调。
 * @param viewModel 驱动本页面状态的 [ProfileViewModel]。默认为作用域内的 ViewModel。
 */
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
    // 退出登录确认对话框是否当前可见。
    var showLogoutDialog by remember { mutableStateOf(false) }

    // 当 showLogoutDialog 为 true 时显示退出登录确认对话框。
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
            // 个人资料头部 — 点击导航至编辑资料页面。
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

            // 菜单项 — 每个导航至不同的子页面。
            ProfileMenuItem(Icons.Default.Receipt, "我的订单", onClick = onOrders)
            ProfileMenuItem(Icons.Default.Download, "下载管理", onClick = onDownloads)
            ProfileMenuItem(Icons.Default.FolderOpen, "离线文件", onClick = onDownloads)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            ProfileMenuItem(Icons.Default.Settings, "设置", onClick = onSettings)
            // "退出登录"菜单项使用错误色，点击后弹出确认对话框而非直接退出。
            ProfileMenuItem(Icons.AutoMirrored.Filled.Logout, "退出登录", onClick = { showLogoutDialog = true }, isDestructive = true)
        }
    }
}

/**
 * 个人资料菜单列表中的单行条目。
 *
 * @param icon 作为前置图标显示的 [ImageVector]。
 * @param title 菜单项的标签文本。
 * @param onClick 点击条目时的回调。
 * @param isDestructive 为 `true` 时，文本和图标以错误色渲染。
 */
@Composable
private fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit, isDestructive: Boolean = false) {
    ListItem(
        headlineContent = { Text(title, color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
        leadingContent = { Icon(icon, null, tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
