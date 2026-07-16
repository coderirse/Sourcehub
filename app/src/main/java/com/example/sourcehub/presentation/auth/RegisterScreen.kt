package com.example.sourcehub.presentation.auth

/**
 * 注册页面组件。
 *
 * 渲染一个可滚动的表单，包含昵称、邮箱、密码和确认密码字段。
 * 包含带返回箭头的顶部应用栏，用户可返回登录页面。
 * 导航事件（注册成功）通过一次性事件通道从 [RegisterViewModel] 流出。
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 注册页面 — 多字段注册表单。
 *
 * @param onNavigateBack 用户点击顶部应用栏中的返回箭头时调用；
 *   通常回退到登录页面。
 * @param onRegisterSuccess 注册成功后调用；
 *   宿主通常导航到首页（已认证）目标图。
 * @param viewModel [RegisterViewModel]，持有表单状态、验证和注册调用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    // 生命周期感知的状态收集。
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 密码字段可见性的局部界面切换。
    var passwordVisible by remember { mutableStateOf(false) }

    // 一次性事件收集器 — 监听来自视图模型的导航触发器。
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is com.example.sourcehub.presentation.common.state.UiEvent.Navigate -> onRegisterSuccess()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            // 带返回箭头的顶部应用栏，用于返回登录页面。
            TopAppBar(
                title = { Text("注册") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        // 可滚动的列，即使在小屏幕上也能容纳所有字段。
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // ---- 显示名称字段 ----
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("昵称") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ---- 邮箱字段 ----
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

            // ---- 密码字段（含可见性切换）----
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                // 在明文和掩码显示之间切换。
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ---- 确认密码字段（始终掩码 — 无切换）----
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("确认密码") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                modifier = Modifier.fillMaxWidth(),
                // 确认字段始终使用掩码视觉变换。
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // ---- 条件错误横幅 ----
            if (uiState.error != null) {
                Text(
                    uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ---- 注册按钮 / 加载旋转器 ----
            Button(
                onClick = viewModel::register,
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
                    Text("注册", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
