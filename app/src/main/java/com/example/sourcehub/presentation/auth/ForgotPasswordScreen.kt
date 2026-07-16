package com.example.sourcehub.presentation.auth

/**
 * 忘记密码页面组件。
 *
 * 一个简单的表单，收集用户注册的邮箱地址，
 * 并通过 [ForgotPasswordViewModel] 触发密码重置邮件。
 * 包含带返回箭头的顶部应用栏，并在文本字段下方内联渲染成功/错误反馈。
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 忘记密码页面 — 单字段邮箱表单。
 *
 * @param onNavigateBack 用户点击顶部应用栏中的返回箭头时调用；
 *   通常将回退栈弹出到登录页面。
 * @param viewModel [ForgotPasswordViewModel]，持有邮箱字段状态、
 *   验证和密码重置 API 调用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel()
) {
    // 生命周期感知的页面界面状态收集。
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            // 带返回箭头的顶部应用栏。
            TopAppBar(
                title = { Text("忘记密码") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        // 居中、单列布局。
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ---- 提示说明 ----
            Text("请输入注册邮箱", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))

            // ---- 邮箱输入字段 ----
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("邮箱") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ---- 成功 / 错误反馈 ----
            // 颜色取决于 [ForgotPasswordUiState.isSuccess]：
            // 成功时为主色，失败时为错误色。
            if (uiState.message != null) {
                Text(
                    uiState.message!!,
                    color = if (uiState.isSuccess) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ---- 提交按钮 ----
            // 请求进行中时标签切换为"发送中..."。
            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isLoading
            ) {
                Text(if (uiState.isLoading) "发送中..." else "发送重置邮件")
            }
        }
    }
}
