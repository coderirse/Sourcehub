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
                actions = { TextButton(onClick = viewModel::save) { Text("保存") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = uiState.name, onValueChange = viewModel::onNameChange, label = { Text("昵称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = uiState.email, onValueChange = {}, label = { Text("邮箱") }, modifier = Modifier.fillMaxWidth(), enabled = false, singleLine = true)
            OutlinedTextField(value = uiState.phone, onValueChange = viewModel::onPhoneChange, label = { Text("手机号") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (uiState.message != null) {
                Text(uiState.message!!, color = if (uiState.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
    }
}
