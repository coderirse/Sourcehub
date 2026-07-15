package com.example.sourcehub.presentation.product.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcehub.presentation.common.components.ErrorView
import com.example.sourcehub.presentation.common.components.LoadingIndicator

private const val PREVIEW_MAX_PAGES = 3
private const val PAGE_WIDTH = 1240
private const val PAGE_HEIGHT = 1754

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPreviewScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProductPreviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val context = LocalContext.current

    DisposableEffect(Unit) {
        view.setWindowSecureFlag(true)
        onDispose { view.setWindowSecureFlag(false) }
    }

    LaunchedEffect(productId) { viewModel.loadPreview(productId) }

    BackHandler(enabled = true) { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预览 - 仅展示前${PREVIEW_MAX_PAGES}页") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(Modifier.padding(padding))
            uiState.error != null -> ErrorView(uiState.error!!, { viewModel.loadPreview(productId) }, Modifier.padding(padding))
            else -> {
                val pageCount = minOf(PREVIEW_MAX_PAGES, maxOf(1, uiState.pageCount))
                val pagerState = rememberPagerState(pageCount = { pageCount })

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // Page indicator
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${pagerState.currentPage + 1} / $pageCount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) { page ->
                        val bitmap = remember(uiState.productTitle, uiState.productDescription, page) {
                            generatePreviewPage(
                                uiState.productTitle,
                                uiState.productDescription,
                                page
                            )
                        }
                        Card(
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            shape = RoundedCornerShape(4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                bitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "预览第${page + 1}页",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                // Watermark overlay
                                Text(
                                    text = "PREVIEW - SourceHub",
                                    color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.12f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(32.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Bottom hint
                    Text(
                        text = "此为预览版本，仅展示部分内容。左右滑动翻页。购买后可下载完整文件。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Generate a preview page bitmap using Canvas.
 * In production, this would be replaced with PdfRenderer rendering.
 */
private fun generatePreviewPage(title: String, description: String, pageIndex: Int): Bitmap? {
    return try {
        val scale = 0.5f // Scale down for memory
        val w = (PAGE_WIDTH * scale).toInt()
        val h = (PAGE_HEIGHT * scale).toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background
        canvas.drawColor(android.graphics.Color.WHITE)

        val textPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 32f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 48f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 24f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 2f
        }

        val padding = 60f
        var y = padding + 60f

        // Header
        canvas.drawText("SourceHub 预览", padding, y, headerPaint)
        y += 50f

        // Line
        canvas.drawLine(padding, y, w - padding, y, linePaint)
        y += 50f

        // Title
        val titleLines = wrapText(title, titlePaint, w - padding * 2)
        titleLines.forEach { line ->
            canvas.drawText(line, padding, y, titlePaint)
            y += 60f
        }
        y += 30f

        // Page-specific content
        when (pageIndex) {
            0 -> {
                canvas.drawText("商品简介", padding, y, textPaint)
                y += 45f
                val descLines = wrapText(description.take(500), textPaint, w - padding * 2)
                descLines.take(18).forEach { line ->
                    canvas.drawText(line, padding, y, textPaint)
                    y += 38f
                }
            }
            1 -> {
                canvas.drawText("内容概览", padding, y, textPaint)
                y += 45f
                val sections = listOf(
                    "1. 概述与适用场景",
                    "2. 核心功能特性",
                    "3. 使用方法与步骤",
                    "4. 常见问题解答",
                    "5. 技术支持与更新"
                )
                sections.forEach { section ->
                    canvas.drawText(section, padding + 20f, y, textPaint)
                    y += 45f
                }
                y += 30f
                canvas.drawText("（完整内容请购买后查看）", padding + 20f, y, textPaint.apply { color = android.graphics.Color.GRAY })
            }
            2 -> {
                canvas.drawText("部分内容节选", padding, y, textPaint)
                y += 45f
                val sampleText = description.ifEmpty { "此为示例预览页面。购买后可获取完整${title}的全部内容。" }
                val sampleLines = wrapText(sampleText.repeat(if (sampleText.length < 100) 3 else 1).take(800), textPaint, w - padding * 2)
                sampleLines.take(20).forEach { line ->
                    canvas.drawText(line, padding, y, textPaint)
                    y += 38f
                }
                y += 40f
                canvas.drawText("... 更多内容请购买后查看 ...", padding, y, textPaint.apply { color = android.graphics.Color.GRAY; textSize = 28f })
            }
        }

        // Footer
        y = h - padding - 30f
        canvas.drawText("SourceHub © ${pageIndex + 1}/$PREVIEW_MAX_PAGES", padding, y, headerPaint)

        bitmap
    } catch (e: Exception) {
        null
    }
}

private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val lines = mutableListOf<String>()
    val words = text.split(" ")
    var currentLine = StringBuilder()
    val rect = Rect()

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        paint.getTextBounds(testLine, 0, testLine.length, rect)
        if (rect.width() > maxWidth && currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
            currentLine = StringBuilder(word)
        } else {
            if (currentLine.isNotEmpty()) currentLine.append(" ")
            currentLine.append(word)
        }
    }
    if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
    return lines
}

private fun android.view.View.setWindowSecureFlag(enable: Boolean) {
    if (enable) {
        (context as? android.app.Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        (context as? android.app.Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
