package com.example.sourcehub.presentation.auth

/**
 * 登录页面组件。
 *
 * 渲染邮箱/密码表单，处理来自 [LoginViewModel] 的导航事件，
 * 并提供指向注册和忘记密码流程的链接。
 * 这是未认证用户的主要入口页面。
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 登录页面 — 邮箱/密码表单，包含导航链接。
 *
 * @param onNavigateToRegister 用户点击"注册账号"时调用。
 * @param onNavigateToForgotPassword 用户点击"忘记密码？"时调用。
 * @param onLoginSuccess 登录成功后调用；宿主通常导航到
 *   首页（已认证）目标图。
 * @param viewModel [LoginViewModel]，持有表单状态和认证逻辑。
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    // 生命周期感知的收集，使界面仅在页面活跃时观察。
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 局部界面状态；不属于视图模型，因为它不影响
    // 业务逻辑 — 仅切换密码字段的可见性掩码。
    var passwordVisible by remember { mutableStateOf(false) }

    // 一次性事件收集器 — 每次组合仅触发一次。
    // 监听视图模型发出的导航触发器。
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is com.example.sourcehub.presentation.common.state.UiEvent.Navigate -> onLoginSuccess()
                else -> {}
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ---- 品牌标识头部 ----
            Text(
                text = "SourceHub",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "优质资料，触手可及",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))

            // ---- 邮箱字段 ----
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("邮箱") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ---- 密码字段（含可见性切换）----
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                // 在明文和掩码（圆点）显示之间切换。
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // ---- 条件错误横幅 ----
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ---- 登录按钮 / 加载旋转器 ----
            Button(
                onClick = viewModel::login,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                // 加载中时禁用，防止重复提交。
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("登录", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ---- 辅助导航链接 ----
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onNavigateToRegister) { Text("注册账号") }
                TextButton(onClick = onNavigateToForgotPassword) { Text("忘记密码？") }
            }
        }
    }
}
