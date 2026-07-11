package com.example.sourcehub.presentation.product.preview

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcehub.presentation.common.components.ErrorView
import com.example.sourcehub.presentation.common.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPreviewScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProductPreviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current

    // Enable FLAG_SECURE to prevent screenshots
    DisposableEffect(Unit) {
        view.setWindowSecureFlag(true)
        onDispose { view.setWindowSecureFlag(false) }
    }

    LaunchedEffect(productId) { viewModel.loadPreview(productId) }

    BackHandler(enabled = true) { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预览 - 仅展示前3页") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(Modifier.padding(padding))
            uiState.error != null -> ErrorView(uiState.error!!, { viewModel.loadPreview(productId) }, Modifier.padding(padding))
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // Placeholder PDF viewer - in production, use PdfRenderer
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "PDF 预览区域",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "${uiState.productTitle}\n\n[预览内容 - 前3页]\n\n此处将集成PdfRenderer展示PDF/文档预览，带半透明水印\"PREVIEW - SourceHub\"",
                                    modifier = Modifier.padding(32.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "PREVIEW - SourceHub",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "此为预览版本，仅展示部分内容。购买后可下载完整文件。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun android.view.View.setWindowSecureFlag(enable: Boolean) {
    if (enable) {
        (context as? android.app.Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        (context as? android.app.Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
