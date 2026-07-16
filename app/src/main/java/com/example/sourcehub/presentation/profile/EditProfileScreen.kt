/**
 * 编辑当前用户个人资料的页面：昵称、邮箱和手机号。
 *
 * 邮箱字段为只读（禁用状态）。保存后表单字段下方
 * 会显示成功或错误消息。
 */
package com.example.sourcehub.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 编辑资料页面组件。
 *
 * @param onNavigateBack 用户点击顶部导航栏返回箭头时的回调。
 * @param viewModel 驱动表单状态并处理保存的 [EditProfileViewModel]。默认为作用域内的 ViewModel。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑资料") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                // "保存"按钮直接触发 ViewModel 的保存操作。
                actions = { TextButton(onClick = viewModel::save) { Text("保存") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 可编辑的昵称字段。
            OutlinedTextField(value = uiState.name, onValueChange = viewModel::onNameChange, label = { Text("昵称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            // 只读邮箱字段 — 用户无法在此处修改邮箱。
            OutlinedTextField(value = uiState.email, onValueChange = {}, label = { Text("邮箱") }, modifier = Modifier.fillMaxWidth(), enabled = false, singleLine = true)
            // 可编辑的手机号字段。
            OutlinedTextField(value = uiState.phone, onValueChange = viewModel::onPhoneChange, label = { Text("手机号") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            // 保存操作后显示的反馈消息（成功或错误）。
            if (uiState.message != null) {
                Text(uiState.message!!, color = if (uiState.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
    }
}
